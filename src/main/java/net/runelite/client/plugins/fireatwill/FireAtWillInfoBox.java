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

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

/**
 * Small-tile (infobox) variant — renders the remaining time as text overlaid on the
 * cannon icon. Active only when {@link FireAtWillConfig#displayMode()} is {@code SMALL_TILE};
 * when {@code LARGE_TILE} is selected the {@link FireAtWillOverlay} is used instead.
 */
public class FireAtWillInfoBox extends InfoBox
{
	private final FireAtWillPlugin plugin;
	private final FireAtWillConfig config;

	public FireAtWillInfoBox(BufferedImage image, FireAtWillPlugin plugin, FireAtWillConfig config)
	{
		super(image, plugin);
		this.plugin = plugin;
		this.config = config;
		setPriority(InfoBoxPriority.MED);
	}

	@Override
	public String getText()
	{
		if (plugin.isLingering())
		{
			return "0:00";
		}
		return formatTime(plugin.getRemainingSeconds());
	}

	@Override
	public Color getTextColor()
	{
		if (plugin.isLingering())
		{
			return Color.RED;
		}

		final long remaining = plugin.getRemainingSeconds();
		if (remaining <= config.alertThreshold())
		{
			return Color.RED;
		}
		if (remaining <= config.warningThreshold())
		{
			return Color.YELLOW;
		}
		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		if (plugin.isLingering())
		{
			return "Crew is not firing at will";
		}
		return "Fire at will: " + formatTime(plugin.getRemainingSeconds()) + " remaining";
	}

	@Override
	public boolean render()
	{
		if (config.displayMode() != FireAtWillConfig.DisplayMode.SMALL_TILE)
		{
			return false;
		}
		return plugin.isActive() || plugin.isLingering();
	}

	private static String formatTime(long totalSeconds)
	{
		final long minutes = totalSeconds / 60;
		final long seconds = totalSeconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}
}
