package net.runelite.client.plugins.spawncounter;

import java.awt.Color;
import net.runelite.client.config.Alpha;
import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.Range;

@ConfigGroup("spawncounter")
public interface SpawnCounterConfig extends Config
{
	@ConfigItem(
		keyName = "npcCountdowns",
		name = "NPC spawn countdowns",
		description = "NPC spawn countdown entries in the format: NPC name, countdown ticks. Separate multiple entries with new lines or semicolons."
	)
	default String npcCountdowns()
	{
		return "";
	}

	@ConfigItem(
		keyName = "npcDeathCountdowns",
		name = "NPC death countdowns",
		description = "NPC death countdown entries in the format: NPC name, countdown ticks. Separate multiple entries with new lines or semicolons."
	)
	default String npcDeathCountdowns()
	{
		return "";
	}

	@ConfigItem(
		keyName = "removeOnDespawn",
		name = "Remove spawn timer on despawn",
		description = "Remove the spawn countdown early when all matching NPCs or objects for that name have despawned."
	)
	default boolean removeOnDespawn()
	{
		return true;
	}

	@ConfigItem(
		keyName = "deathCountdownMode",
		name = "Death countdown mode",
		description = "Choose whether death countdown overlays are shown in game ticks or seconds."
	)
	default CountdownMode deathCountdownMode()
	{
		return CountdownMode.TICKS;
	}

	@Alpha
	@ConfigItem(
		keyName = "deathTextColor",
		name = "Death text color",
		description = "Color of death countdown text."
	)
	default Color deathTextColor()
	{
		return Color.WHITE;
	}

	@Alpha
	@ConfigItem(
		keyName = "deathTileOutlineColor",
		name = "Death tile outline",
		description = "Color of death countdown tile outline."
	)
	default Color deathTileOutlineColor()
	{
		return Color.CYAN;
	}

	@Alpha
	@ConfigItem(
		keyName = "deathTileFillColor",
		name = "Death tile fill",
		description = "Fill color of death countdown tile."
	)
	default Color deathTileFillColor()
	{
		return new Color(0, 255, 255, 20);
	}

	@Range(min = 1, max = 10)
	@ConfigItem(
		keyName = "deathTileWidth",
		name = "Death tile width",
		description = "Width of death countdown tile outline."
	)
	default int deathTileWidth()
	{
		return 2;
	}

	@ConfigItem(
		keyName = "showDeathTile",
		name = "Show death tile",
		description = "Draws a tile marker underneath death countdowns."
	)
	default boolean showDeathTile()
	{
		return true;
	}

	@ConfigItem(
		keyName = "showDeathName",
		name = "Show death NPC name",
		description = "Displays the NPC name above death countdowns."
	)
	default boolean showDeathName()
	{
		return false;
	}

	enum CountdownMode
	{
		TICKS,
		SECONDS
	}
}
