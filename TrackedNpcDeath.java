package net.runelite.client.plugins.deathcounter;

import lombok.Getter;
import lombok.Setter;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

@Getter
@Setter
public class TrackedNpcDeath
{
	private final int index;
	private final int id;
	private final String name;
	private final int size;

	private WorldPoint spawnPoint;
	private int diedOnTick = -1;
	private boolean dead;

	public TrackedNpcDeath(NPC npc)
	{
		index = npc.getIndex();
		id = npc.getId();
		name = npc.getName();

		NPCComposition composition = npc.getTransformedComposition();
		size = composition != null ? composition.getSize() : 1;
	}

	public boolean matches(NPC npc)
	{
		return npc.getIndex() == index && npc.getId() == id;
	}

	public boolean hasSpawnPoint()
	{
		return spawnPoint != null;
	}
}
