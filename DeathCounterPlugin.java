package net.runelite.client.plugins.deathcounter;

import com.google.inject.Provides;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcDespawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.Text;
import net.runelite.client.util.WildcardMatcher;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Death Counter",
	description = "Shows a countdown overlay where selected NPCs die"
)
public class DeathCounterPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private DeathCounterOverlay overlay;

	@Inject
	private DeathCounterConfig config;

	@Getter
	private final List<TrackedNpcDeath> trackedDeaths = new ArrayList<>();

	@Getter
	private Instant lastTickUpdate = Instant.now();

	@Provides
	DeathCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DeathCounterConfig.class);
	}

	@Override
	protected void startUp()
	{
		clientThread.invokeLater(() ->
		{
			trackedDeaths.clear();
			lastTickUpdate = Instant.now();
			overlayManager.add(overlay);
		});
	}

	@Override
	protected void shutDown()
	{
		trackedDeaths.clear();
		overlayManager.remove(overlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			trackedDeaths.clear();
		}
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		if (!npc.isDead() || !isConfiguredNpc(npc))
		{
			return;
		}

		TrackedNpcDeath death = getOrCreateTrackedDeath(npc);
		WorldPoint deathPoint = getNpcWorldPoint(npc);
		if (deathPoint != null)
		{
			death.setSpawnPoint(deathPoint);
		}

		death.setDiedOnTick(client.getTickCount());
		death.setDead(true);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickUpdate = Instant.now();
		trackedDeaths.removeIf(death -> death.isDead() && getTicksRemaining(death) <= 0);
	}

	public int getTicksRemaining(TrackedNpcDeath death)
	{
		if (death.getDiedOnTick() < 0)
		{
			return -1;
		}

		return Math.max(0, config.timerTicks() - (client.getTickCount() - death.getDiedOnTick()));
	}

	private TrackedNpcDeath getOrCreateTrackedDeath(NPC npc)
	{
		TrackedNpcDeath death = trackedDeaths.stream()
			.filter(d -> d.matches(npc))
			.findFirst()
			.orElse(null);

		if (death == null)
		{
			death = new TrackedNpcDeath(npc);
			trackedDeaths.add(death);
		}

		return death;
	}

	private boolean isConfiguredNpc(NPC npc)
	{
		boolean hasNameFilters = !config.npcNames().trim().isEmpty();
		boolean hasIdFilters = !config.npcIds().trim().isEmpty();
		if (!hasNameFilters && !hasIdFilters)
		{
			return true;
		}

		if (hasIdFilters)
		{
			String npcId = Integer.toString(npc.getId());
			for (String id : config.npcIds().split(","))
			{
				if (npcId.equals(id.trim()))
				{
					return true;
				}
			}
		}

		if (hasNameFilters && npc.getName() != null)
		{
			String npcName = Text.removeTags(npc.getName()).toLowerCase();
			for (String name : config.npcNames().split(","))
			{
				String filter = name.trim().toLowerCase();
				if (!filter.isEmpty() && WildcardMatcher.matches(filter, npcName))
				{
					return true;
				}
			}
		}

		return false;
	}

	private WorldPoint getNpcWorldPoint(NPC npc)
	{
		return client.isInInstancedRegion()
			? WorldPoint.fromLocalInstance(client, npc.getLocalLocation())
			: WorldPoint.fromLocal(client, npc.getLocalLocation());
	}
}
