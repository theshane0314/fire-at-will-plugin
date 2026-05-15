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

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Units;

@ConfigGroup(FireAtWillConfig.GROUP)
public interface FireAtWillConfig extends Config
{
	String GROUP = "fireatwilltimer";

	enum DisplayMode
	{
		LARGE_TILE,
		SMALL_TILE
	}

	enum NotifyOn
	{
		WARNING,
		ALERT,
		EXPIRY
	}

	@ConfigSection(
		name = "Display",
		description = "Configure how the timer is displayed",
		position = 0
	)
	String displaySection = "display";

	@ConfigSection(
		name = "Thresholds",
		description = "Configure when the timer changes color",
		position = 1
	)
	String thresholdSection = "thresholds";

	@ConfigSection(
		name = "Notification",
		description = "Configure how and when you are notified",
		position = 2
	)
	String notificationSection = "notification";

	@ConfigItem(
		keyName = "displayMode",
		name = "Display mode",
		description = "Show the timer as a large tile (like Opponent Information) or a small tile (like Boost Information)",
		section = displaySection,
		position = 0
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.SMALL_TILE;
	}

	@ConfigItem(
		keyName = "warningThreshold",
		name = "Warning threshold",
		description = "The timer turns yellow when this many seconds remain",
		section = thresholdSection,
		position = 0
	)
	@Units(Units.SECONDS)
	default int warningThreshold()
	{
		return 30;
	}

	@ConfigItem(
		keyName = "alertThreshold",
		name = "Alert threshold",
		description = "The timer turns red when this many seconds remain",
		section = thresholdSection,
		position = 1
	)
	@Units(Units.SECONDS)
	default int alertThreshold()
	{
		return 10;
	}

	@ConfigItem(
		keyName = "notifyOn",
		name = "Notify on",
		description = "When to fire the notification — at the warning threshold, alert threshold, or when the timer expires",
		section = notificationSection,
		position = 0
	)
	default NotifyOn notifyOn()
	{
		return NotifyOn.EXPIRY;
	}

	@ConfigItem(
		keyName = "notification",
		name = "Notification",
		description = "Open the override panel to configure sound, flash, color, system tray, and chat options",
		section = notificationSection,
		position = 1
	)
	default Notification notification()
	{
		return Notification.ON;
	}
}
