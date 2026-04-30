package net.runelite.client.plugins.deathcounter;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup(DeathCounterConfig.CONFIG_GROUP)
public interface DeathCounterConfig extends Config
{
	String CONFIG_GROUP = "deathcounter";

	@ConfigItem(
		position = 0,
		keyName = "countdownMode",
		name = "Countdown Mode",
		description = "Choose whether the countdown is shown in game ticks or seconds"
	)
	default CountdownMode countdownMode()
	{
		return CountdownMode.TICKS;
	}

	@Alpha
	@ConfigItem(
		position = 1,
		keyName = "textColor",
		name = "Text Color",
		description = "Color of the countdown text"
	)
	default Color textColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		position = 2,
		keyName = "tileOutlineColor",
		name = "Tile Outline Color",
		description = "Color of the countdown tile outline"
	)
	default Color tileOutlineColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		position = 3,
		keyName = "tileFillColor",
		name = "Tile Fill Color",
		description = "Fill color of the countdown tile"
	)
	default Color tileFillColor()
	{
		return new Color(0, 255, 255, 20);
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		position = 4,
		keyName = "tileWidth",
		name = "Tile Width",
		description = "Width of the countdown tile outline"
	)
	default int tileWidth()
	{
		return 2;
	}

	@ConfigItem(
		position = 5,
		keyName = "timerTicks",
		name = "Timer Ticks",
		description = "Countdown length to show when an NPC dies"
	)
	default int timerTicks()
	{
		return 25;
	}

	@ConfigItem(
		position = 6,
		keyName = "npcNames",
		name = "NPC Names",
		description = "NPC names to show death timers for, separated by commas. Supports wildcards like goblin*"
	)
	default String npcNames()
	{
		return "";
	}

	@ConfigItem(
		position = 7,
		keyName = "npcIds",
		name = "NPC IDs",
		description = "NPC IDs to show death timers for, separated by commas"
	)
	default String npcIds()
	{
		return "";
	}

	@ConfigItem(
		position = 8,
		keyName = "showTile",
		name = "Show Tile",
		description = "Draws a tile marker underneath the countdown"
	)
	default boolean showTile()
	{
		return true;
	}

	@ConfigItem(
		position = 9,
		keyName = "showName",
		name = "Show NPC Name",
		description = "Displays the NPC name above the countdown"
	)
	default boolean showName()
	{
		return false;
	}

	enum CountdownMode
	{
		TICKS,
		SECONDS
	}
}
