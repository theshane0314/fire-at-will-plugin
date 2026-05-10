package net.runelite.client.plugins.fireatwill;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

public class FireAtWillOverlay extends OverlayPanel
{
	private static final Color TITLE_COLOR = new Color(255, 165, 0);
	private static final Color EXPIRED_COLOR = Color.RED;

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

		panelComponent.getChildren().clear();

		if (plugin.isLingering())
		{
			panelComponent.getChildren().add(TitleComponent.builder()
				.text("Fire at Will")
				.color(TITLE_COLOR)
				.build());

			panelComponent.getChildren().add(LineComponent.builder()
				.left("Crew not firing at will")
				.leftColor(EXPIRED_COLOR)
				.build());

			return super.render(graphics);
		}

		if (!plugin.isActive())
		{
			return null;
		}

		long remaining = plugin.getRemainingSeconds();
		long minutes = remaining / 60;
		long seconds = remaining % 60;
		String timeText = String.format("%d:%02d", minutes, seconds);

		Color timeColor;
		if (remaining <= config.alertThreshold())
		{
			timeColor = Color.RED;
		}
		else if (remaining <= config.warningThreshold())
		{
			timeColor = Color.YELLOW;
		}
		else
		{
			timeColor = Color.WHITE;
		}

		panelComponent.getChildren().add(TitleComponent.builder()
			.text("Fire at Will")
			.color(TITLE_COLOR)
			.build());

		panelComponent.getChildren().add(LineComponent.builder()
			.left("Time Remaining")
			.leftColor(Color.WHITE)
			.right(timeText)
			.rightColor(timeColor)
			.build());

		return super.render(graphics);
	}
}
