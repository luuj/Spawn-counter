package net.runelite.client.plugins.spawncounter;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

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
}
