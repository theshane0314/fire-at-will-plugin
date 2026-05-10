package net.runelite.client.plugins.fireatwill;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxPriority;

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

		long remaining = plugin.getRemainingSeconds();
		long minutes = remaining / 60;
		long seconds = remaining % 60;
		return String.format("%d:%02d", minutes, seconds);
	}

	@Override
	public Color getTextColor()
	{
		if (plugin.isLingering())
		{
			return Color.RED;
		}

		long remaining = plugin.getRemainingSeconds();
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

		long remaining = plugin.getRemainingSeconds();
		long minutes = remaining / 60;
		long seconds = remaining % 60;
		return String.format("Fire at Will: %d:%02d remaining", minutes, seconds);
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
}
