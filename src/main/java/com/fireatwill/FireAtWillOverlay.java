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
package com.fireatwill;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/**
 * Large-tile overlay variant — renders a titled panel with the remaining time.
 * Active only when {@link FireAtWillConfig#displayMode()} is {@code LARGE_TILE}; when
 * {@code SMALL_TILE} is selected the {@link FireAtWillInfoBox} is used instead.
 */
public class FireAtWillOverlay extends OverlayPanel
{
	private static final Color TITLE_COLOR = new Color(255, 165, 0);
	private static final String TITLE_TEXT = "Fire at Will";

	private final FireAtWillPlugin plugin;
	private final FireAtWillConfig config;

	@Inject
	public FireAtWillOverlay(FireAtWillPlugin plugin, FireAtWillConfig config)
	{
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.TOP_LEFT);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		if (config.displayMode() != FireAtWillConfig.DisplayMode.LARGE_TILE)
		{
			return null;
		}

		// During the linger window after expiry, show a static "expired" state instead of the timer.
		if (plugin.isLingering())
		{
			panelComponent.getChildren().clear();
			panelComponent.getChildren().add(TitleComponent.builder()
				.text(TITLE_TEXT)
				.color(TITLE_COLOR)
				.build());
			panelComponent.getChildren().add(LineComponent.builder()
				.left("Crew not firing at will")
				.leftColor(Color.RED)
				.build());
			return super.render(graphics);
		}

		if (!plugin.isActive())
		{
			return null;
		}

		final long remaining = plugin.getRemainingSeconds();
		final String timeText = formatTime(remaining);
		final Color timeColor = colorForRemaining(remaining);

		panelComponent.getChildren().clear();
		panelComponent.getChildren().add(TitleComponent.builder()
			.text(TITLE_TEXT)
			.color(TITLE_COLOR)
			.build());
		panelComponent.getChildren().add(LineComponent.builder()
			.left("Time remaining")
			.leftColor(Color.WHITE)
			.right(timeText)
			.rightColor(timeColor)
			.build());

		return super.render(graphics);
	}

	private Color colorForRemaining(long remaining)
	{
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

	private static String formatTime(long totalSeconds)
	{
		final long minutes = totalSeconds / 60;
		final long seconds = totalSeconds % 60;
		return String.format("%d:%02d", minutes, seconds);
	}
}
