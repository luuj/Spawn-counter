package net.runelite.client.plugins.spawncounter;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.time.Instant;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.Text;

class SpawnCounterDeathOverlay extends Overlay
{
	private final Client client;
	private final SpawnCounterPlugin plugin;
	private final SpawnCounterConfig config;

	@Inject
	SpawnCounterDeathOverlay(Client client, SpawnCounterPlugin plugin, SpawnCounterConfig config)
	{
		this.client = client;
		this.plugin = plugin;
		this.config = config;
		setPosition(OverlayPosition.DYNAMIC);
		setLayer(OverlayLayer.ABOVE_SCENE);
	}

	@Override
	public Dimension render(Graphics2D graphics)
	{
		for (TrackedNpcDeath death : plugin.getTrackedDeaths())
		{
			int ticksRemaining = plugin.getTicksRemaining(death);
			if (!death.hasSpawnPoint() || ticksRemaining <= 0)
			{
				continue;
			}

			LocalPoint localPoint = LocalPoint.fromWorld(client, death.getSpawnPoint());
			if (localPoint == null)
			{
				continue;
			}

			LocalPoint centerPoint = new LocalPoint(
				localPoint.getX() + Perspective.LOCAL_TILE_SIZE * (death.getSize() - 1) / 2,
				localPoint.getY() + Perspective.LOCAL_TILE_SIZE * (death.getSize() - 1) / 2
			);

			if (config.showDeathTile())
			{
				renderTile(graphics, centerPoint, death.getSize());
			}

			String text = getCountdownText(ticksRemaining);
			if (config.showDeathName() && death.getName() != null)
			{
				text = Text.removeTags(death.getName()) + ": " + text;
			}

			Point textLocation = Perspective.getCanvasTextLocation(client, graphics, centerPoint, text, 0);
			if (textLocation != null)
			{
				OverlayUtil.renderTextLocation(graphics, new Point(textLocation.getX() + 1, textLocation.getY() + 1), text, Color.BLACK);
				OverlayUtil.renderTextLocation(graphics, textLocation, text, config.deathTextColor());
			}
		}

		return null;
	}

	private void renderTile(Graphics2D graphics, LocalPoint centerPoint, int size)
	{
		Polygon tile = Perspective.getCanvasTileAreaPoly(client, centerPoint, size);
		if (tile == null)
		{
			return;
		}

		Color outline = config.deathTileOutlineColor();
		Color fill = config.deathTileFillColor();
		graphics.setColor(new Color(outline.getRed(), outline.getGreen(), outline.getBlue(), outline.getAlpha()));
		graphics.setStroke(new BasicStroke(config.deathTileWidth()));
		graphics.draw(tile);
		graphics.setColor(new Color(fill.getRed(), fill.getGreen(), fill.getBlue(), fill.getAlpha()));
		graphics.fill(tile);
	}

	private String getCountdownText(int ticksRemaining)
	{
		if (config.deathCountdownMode() == SpawnCounterConfig.CountdownMode.TICKS)
		{
			return Integer.toString(ticksRemaining);
		}

		Instant now = Instant.now();
		double baseSeconds = ticksRemaining * (Constants.GAME_TICK_LENGTH / 1000.0);
		double secondsSinceTick = (now.toEpochMilli() - plugin.getLastTickUpdate().toEpochMilli()) / 1000.0;
		return String.format("%.1f", Math.max(0, baseSeconds - secondsSinceTick));
	}
}
