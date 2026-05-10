package net.runelite.client.plugins.fireatwill;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Notification;
import net.runelite.client.config.Units;

@ConfigGroup("fireatewill")
public interface FireAtWillConfig extends Config
{
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

	@ConfigItem(
		keyName = "displayMode",
		name = "Display Mode",
		description = "Show timer as a large tile (like Opponent Information) or small tile (like Boost Information)",
		section = displaySection,
		position = 0
	)
	default DisplayMode displayMode()
	{
		return DisplayMode.SMALL_TILE;
	}

	@ConfigSection(
		name = "Thresholds",
		description = "Configure when the timer changes color",
		position = 1
	)
	String thresholdSection = "thresholds";

	@ConfigItem(
		keyName = "warningThreshold",
		name = "Warning Threshold",
		description = "Timer turns yellow when this many seconds remain",
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
		name = "Alert Threshold",
		description = "Timer turns red when this many seconds remain",
		section = thresholdSection,
		position = 1
	)
	@Units(Units.SECONDS)
	default int alertThreshold()
	{
		return 10;
	}

	@ConfigSection(
		name = "Notification",
		description = "Configure how and when you are notified",
		position = 2
	)
	String notificationSection = "notification";

	@ConfigItem(
		keyName = "notifyOn",
		name = "Notify On",
		description = "When to trigger the notification — at the Warning threshold, Alert threshold, or when the timer expires",
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
		description = "Notification configuration — open the override panel to set sound, flash, tray, and chat options",
		section = notificationSection,
		position = 1
	)
	default Notification notification()
	{
		return Notification.ON;
	}
}
