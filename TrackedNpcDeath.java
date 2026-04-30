package net.runelite.client.plugins.spawncounter;

import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.coords.WorldPoint;

class TrackedNpcDeath
{
	private final int index;
	private final int id;
	private final String name;
	private final int size;
	private final int countdownTicks;
	private WorldPoint spawnPoint;
	private int diedOnTick = -1;

	TrackedNpcDeath(NPC npc, int countdownTicks)
	{
		index = npc.getIndex();
		id = npc.getId();
		name = npc.getName();
		this.countdownTicks = countdownTicks;

		NPCComposition composition = npc.getTransformedComposition();
		size = composition != null ? composition.getSize() : 1;
	}

	TrackedNpcDeath(String name, int size, int countdownTicks)
	{
		index = -1;
		id = -1;
		this.name = name;
		this.size = size;
		this.countdownTicks = countdownTicks;
	}

	boolean matches(NPC npc)
	{
		return npc.getIndex() == index && npc.getId() == id;
	}

	String getName()
	{
		return name;
	}

	int getSize()
	{
		return size;
	}

	int getCountdownTicks()
	{
		return countdownTicks;
	}

	WorldPoint getSpawnPoint()
	{
		return spawnPoint;
	}

	void setSpawnPoint(WorldPoint spawnPoint)
	{
		this.spawnPoint = spawnPoint;
	}

	int getDiedOnTick()
	{
		return diedOnTick;
	}

	void setDiedOnTick(int diedOnTick)
	{
		this.diedOnTick = diedOnTick;
	}

	boolean hasSpawnPoint()
	{
		return spawnPoint != null;
	}
}
