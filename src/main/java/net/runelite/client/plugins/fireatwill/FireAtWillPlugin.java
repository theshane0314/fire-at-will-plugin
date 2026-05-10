/*
 * Copyright (c) 2026, Shalysa
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.fireatwill;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.Notifier;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.ImageUtil;

/**
 * Tracks the duration of the in-game "Fire at will!" crew call during boat combat
 * (Sailing). When the call is heard in chat, a 10 minute timer begins. The plugin
 * shows the remaining time as either a large tile overlay or a small infobox, and
 * fires a configurable notification at the chosen moment (warning, alert, or expiry).
 *
 * The timer also stops cleanly when the player issues an overriding crew call
 * (Attack my targets! / Await further orders!) or when the game prints the natural
 * end-of-call message.
 */
@Slf4j
@PluginDescriptor(
	name = "Fire at will Timer",
	description = "Tracks how long the Fire at will! crew call remains active and notifies before it expires",
	tags = {"fire", "will", "timer", "sailing", "boat", "crew", "notify"}
)
public class FireAtWillPlugin extends Plugin
{
	/** Duration of the fire at will buff in seconds (10 minutes). */
	static final int TIMER_DURATION_SECONDS = 600;

	/** How long the infobox / overlay continues to display "expired" state after the timer ends, in seconds. */
	static final int LINGER_SECONDS = 300;

	// Chat message fragments — compared against lowercase text via String#contains so trailing
	// punctuation / colour codes don't matter.
	private static final String MSG_START = "fire at will!";
	private static final String MSG_END = "your crew takes a break from firing at will";

	/** Player-issued crew calls that override / cancel an active fire at will. */
	private static final String[] OVERRIDE_CALLS = {
		"attack my targets!",
		"await further orders!"
	};

	// Messages used as the body of the Notifier#notify call.
	private static final String EXPIRY_MESSAGE = "Crew is no longer firing at will";
	private static final String WARNING_MESSAGE = "Fire at will is about to expire";
	private static final String ALERT_MESSAGE = "Fire at will is about to expire!";

	private static final String ICON_RESOURCE = "fire_at_will_icon.png";

	@Inject
	private FireAtWillConfig config;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private Notifier notifier;

	@Inject
	private FireAtWillOverlay timerOverlay;

	/** Cached infobox icon — loaded once at startup so we don't re-decode the PNG every timer cycle. */
	private BufferedImage iconImage;

	private FireAtWillInfoBox infoBox;

	private boolean active;
	private Instant startTime;
	private Instant expiredAt;

	// One-shot flags so each notification only fires once per timer cycle.
	private boolean warningNotified;
	private boolean alertNotified;
	private boolean expiryFired;

	@Provides
	FireAtWillConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(FireAtWillConfig.class);
	}

	@Override
	protected void startUp()
	{
		iconImage = ImageUtil.loadImageResource(getClass(), ICON_RESOURCE);
		overlayManager.add(timerOverlay);
		// Defensive — ensure no timer state survives a plugin reload.
		resetAllState();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(timerOverlay);
		resetAllState();
		iconImage = null;
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		final String msg = event.getMessage().toLowerCase();

		if (!active && msg.contains(MSG_START))
		{
			startTimer();
		}
		else if (active && msg.contains(MSG_END))
		{
			handleExpiry();
			stopTimer();
		}
		else if (active && isOverrideCall(msg))
		{
			// Player swapped to a different crew call — the buff is gone, but no notification fires
			// because this is an intentional cancellation, not a timeout.
			expiredAt = null;
			stopTimer();
		}
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		// Cleanup lingering "expired" state once the linger window has elapsed.
		if (!active && expiredAt != null
			&& Duration.between(expiredAt, Instant.now()).getSeconds() >= LINGER_SECONDS)
		{
			expiredAt = null;
			removeInfoBox();
			return;
		}

		if (!active)
		{
			return;
		}

		final long remaining = getRemainingSeconds();

		if (remaining <= 0)
		{
			handleExpiry();
			stopTimer();
			return;
		}

		if (!warningNotified && remaining <= config.warningThreshold())
		{
			warningNotified = true;
			if (config.notifyOn() == FireAtWillConfig.NotifyOn.WARNING)
			{
				notifier.notify(config.notification(), WARNING_MESSAGE);
			}
		}

		if (!alertNotified && remaining <= config.alertThreshold())
		{
			alertNotified = true;
			if (config.notifyOn() == FireAtWillConfig.NotifyOn.ALERT)
			{
				notifier.notify(config.notification(), ALERT_MESSAGE);
			}
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		// Wipe state on logout / world hop / disconnect so the timer does not appear to be active
		// when the player logs back in.
		final GameState state = event.getGameState();
		if (state == GameState.LOGIN_SCREEN
			|| state == GameState.HOPPING
			|| state == GameState.CONNECTION_LOST)
		{
			resetAllState();
		}
	}

	private void startTimer()
	{
		active = true;
		startTime = Instant.now();
		expiredAt = null;
		warningNotified = false;
		alertNotified = false;
		expiryFired = false;
		addInfoBox();
	}

	private void stopTimer()
	{
		active = false;
		startTime = null;
		// Only tear down the infobox immediately if there is nothing to linger on. When expiredAt
		// is non-null the gametick handler removes it after LINGER_SECONDS.
		if (expiredAt == null)
		{
			removeInfoBox();
		}
	}

	private void handleExpiry()
	{
		if (expiryFired)
		{
			return;
		}
		expiryFired = true;
		expiredAt = Instant.now();

		if (config.notifyOn() == FireAtWillConfig.NotifyOn.EXPIRY)
		{
			notifier.notify(config.notification(), EXPIRY_MESSAGE);
		}
	}

	private void resetAllState()
	{
		active = false;
		startTime = null;
		expiredAt = null;
		warningNotified = false;
		alertNotified = false;
		expiryFired = false;
		removeInfoBox();
	}

	private void addInfoBox()
	{
		removeInfoBox();
		if (iconImage == null)
		{
			// Defensive — should never happen because startUp loads the icon, but avoid NPE if
			// something goes sideways.
			log.warn("Fire at will icon failed to load — skipping infobox");
			return;
		}
		infoBox = new FireAtWillInfoBox(iconImage, this, config);
		infoBoxManager.addInfoBox(infoBox);
	}

	private void removeInfoBox()
	{
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
			infoBox = null;
		}
	}

	private static boolean isOverrideCall(String msg)
	{
		for (String call : OVERRIDE_CALLS)
		{
			if (msg.contains(call))
			{
				return true;
			}
		}
		return false;
	}

	/** @return true while the 10 minute timer is counting down. */
	public boolean isActive()
	{
		return active;
	}

	/**
	 * @return true while the timer has expired but is still within the linger window. The infobox
	 * stays visible during this period to make the expiry visible to the player.
	 */
	public boolean isLingering()
	{
		return expiredAt != null
			&& Duration.between(expiredAt, Instant.now()).getSeconds() < LINGER_SECONDS;
	}

	/** @return seconds remaining on the timer, or 0 if the timer is not active. */
	public long getRemainingSeconds()
	{
		if (!active || startTime == null)
		{
			return 0;
		}
		final long elapsed = Duration.between(startTime, Instant.now()).getSeconds();
		return Math.max(0, TIMER_DURATION_SECONDS - elapsed);
	}
}
