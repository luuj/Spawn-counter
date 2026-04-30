package net.runelite.client.plugins.spawncounter;

import java.awt.Color;
import java.awt.image.BufferedImage;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.overlay.infobox.InfoBox;

class SpawnCountdownInfoBox extends InfoBox
{
	private final String npcName;
	private int ticksRemaining;

	SpawnCountdownInfoBox(BufferedImage image, Plugin plugin, String npcName, int ticksRemaining)
	{
		super(image, plugin);
		this.npcName = npcName;
		this.ticksRemaining = ticksRemaining;
	}

	boolean tick()
	{
		ticksRemaining--;
		return ticksRemaining <= 0;
	}

	@Override
	public String getText()
	{
		return Integer.toString(ticksRemaining);
	}

	@Override
	public Color getTextColor()
	{
		if (ticksRemaining <= 5)
		{
			return Color.RED;
		}

		if (ticksRemaining <= 10)
		{
			return Color.ORANGE;
		}

		return Color.WHITE;
	}

	@Override
	public String getTooltip()
	{
		return npcName + " spawn countdown: " + ticksRemaining + " ticks";
	}
}
