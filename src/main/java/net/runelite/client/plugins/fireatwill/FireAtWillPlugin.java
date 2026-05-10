package net.runelite.client.plugins.fireatwill;

import com.google.inject.Provides;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import javax.inject.Inject;
import lombok.Getter;
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

@Slf4j
@PluginDescriptor(
	name = "Fire at will Timer",
	description = "Tracks how long Fire at Will remains active and notifies you before it expires",
	tags = {"fire", "will", "timer", "cannon", "pvm", "notify"}
)
public class FireAtWillPlugin extends Plugin
{
	static final int TIMER_DURATION_SECONDS = 600;
	static final int LINGER_SECONDS = 300;

	private static final String MSG_START = "fire at will!";
	private static final String MSG_END = "your crew takes a break from firing at will";

	// player-issued crew calls that override / cancel fire at will
	private static final String[] OVERRIDE_CALLS = {
		"attack my targets!",
		"await further orders!"
	};

	static final String EXPIRY_MESSAGE = "Crew is no longer firing at will";
	static final String WARNING_MESSAGE = "Fire at Will is about to expire";
	static final String ALERT_MESSAGE = "Fire at Will is about to expire!";

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

	private FireAtWillInfoBox infoBox;

	@Getter
	private boolean active;

	@Getter
	private Instant startTime;

	@Getter
	private Instant expiredAt;

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
		overlayManager.add(timerOverlay);
		// defensive: clean state on plugin start so nothing leaks from a prior session
		resetAllState();
	}

	@Override
	protected void shutDown()
	{
		overlayManager.remove(timerOverlay);
		expiredAt = null;
		stopTimer();
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		log.debug("ChatMessage [{}]: {}", event.getType(), event.getMessage());
		String msg = event.getMessage().toLowerCase();

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
			// player issued a different crew call — fire at will is off, no expiry alert
			expiredAt = null;
			stopTimer();
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

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		GameState state = event.getGameState();
		// on login screen / hop / connection lost — wipe state so the timer is inactive on next login
		if (state == GameState.LOGIN_SCREEN
			|| state == GameState.HOPPING
			|| state == GameState.CONNECTION_LOST)
		{
			resetAllState();
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

	@Subscribe
	public void onGameTick(GameTick event)
	{
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

		long remaining = getRemainingSeconds();

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

	private void addInfoBox()
	{
		removeInfoBox();
		BufferedImage icon = ImageUtil.loadImageResource(getClass(), "fire_at_will_icon.png");
		infoBox = new FireAtWillInfoBox(icon, this, config);
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

	public boolean isLingering()
	{
		return expiredAt != null
			&& Duration.between(expiredAt, Instant.now()).getSeconds() < LINGER_SECONDS;
	}

	public long getRemainingSeconds()
	{
		if (!active || startTime == null)
		{
			return 0;
		}
		long elapsed = Duration.between(startTime, Instant.now()).getSeconds();
		return Math.max(0, TIMER_DURATION_SECONDS - elapsed);
	}
}
