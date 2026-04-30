package net.runelite.client.plugins.spawncounter;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcChanged;
import net.runelite.api.events.NpcDespawned;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Spawn Counter",
	description = "Shows an infobox when configured NPCs spawn and a scene overlay when they die",
	tags = {"npc", "spawn", "death", "timer", "counter", "infobox"}
)
public class SpawnCounterPlugin extends Plugin
{
	private static final String SPAWN = "spawn";

	private final Map<String, SpawnCountdownInfoBox> activeCountdowns = new HashMap<>();
	private final Map<String, Integer> matchingSpawnCounts = new HashMap<>();
	private final Map<NPC, String> trackedNpcs = new IdentityHashMap<>();
	private final Map<GameObject, String> trackedGameObjects = new IdentityHashMap<>();
	private final List<TrackedNpcDeath> trackedDeaths = new ArrayList<>();

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpawnCounterConfig config;

	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private SpawnCounterDeathOverlay deathOverlay;

	private BufferedImage icon;
	private Instant lastTickUpdate = Instant.now();

	@Override
	protected void startUp()
	{
		icon = createIcon();
		clientThread.invokeLater(() ->
		{
			trackedDeaths.clear();
			lastTickUpdate = Instant.now();
			overlayManager.add(deathOverlay);
		});
	}

	@Override
	protected void shutDown()
	{
		removeAllCountdowns();
		overlayManager.remove(deathOverlay);
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING)
		{
			removeAllCountdowns();
		}
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		trackNpc(event.getNpc());
	}

	@Subscribe
	public void onNpcChanged(NpcChanged event)
	{
		trackNpc(event.getNpc());
	}

	@Subscribe
	public void onNpcDespawned(NpcDespawned event)
	{
		NPC npc = event.getNpc();
		String normalizedName = trackedNpcs.remove(npc);
		untrackSpawn(normalizedName);
		if (npc.isDead() || hasDeathCountdown(normalizedName))
		{
			trackNpcDeath(npc, normalizedName);
		}
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		trackGameObject(event.getGameObject());
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		GameObject gameObject = event.getGameObject();
		String normalizedName = trackedGameObjects.remove(gameObject);
		untrackSpawn(normalizedName);
		trackGameObjectDeath(gameObject, normalizedName);
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
		lastTickUpdate = Instant.now();
		trackedDeaths.removeIf(death -> getTicksRemaining(death) <= 0);

		Iterator<Map.Entry<String, SpawnCountdownInfoBox>> iterator = activeCountdowns.entrySet().iterator();
		while (iterator.hasNext())
		{
			SpawnCountdownInfoBox infoBox = iterator.next().getValue();
			if (infoBox.tick())
			{
				infoBoxManager.removeInfoBox(infoBox);
				iterator.remove();
			}
		}
	}

	@Provides
	SpawnCounterConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(SpawnCounterConfig.class);
	}

	List<TrackedNpcDeath> getTrackedDeaths()
	{
		return trackedDeaths;
	}

	Instant getLastTickUpdate()
	{
		return lastTickUpdate;
	}

	int getTicksRemaining(TrackedNpcDeath death)
	{
		return death.getDiedOnTick() < 0 ? -1 : Math.max(0, death.getCountdownTicks() - (client.getTickCount() - death.getDiedOnTick()));
	}

	private void trackNpc(NPC npc)
	{
		if (trackedNpcs.containsKey(npc) || npc.getName() == null)
		{
			return;
		}

		String normalizedName = normalizeName(npc.getName());
		CountdownTarget spawnTarget = targetsByName(config.npcCountdowns()).get(normalizedName);
		if (spawnTarget == null && !hasDeathCountdown(normalizedName))
		{
			return;
		}

		trackedNpcs.put(npc, normalizedName);
		if (spawnTarget != null)
		{
			trackSpawn(spawnTarget);
		}
	}

	private void trackGameObject(GameObject gameObject)
	{
		if (trackedGameObjects.containsKey(gameObject))
		{
			return;
		}

		String objectName = getGameObjectName(gameObject);
		if (objectName == null)
		{
			return;
		}

		String normalizedName = normalizeName(objectName);
		CountdownTarget spawnTarget = targetsByName(config.npcCountdowns()).get(normalizedName);
		if (spawnTarget == null && !hasDeathCountdown(normalizedName))
		{
			return;
		}

		trackedGameObjects.put(gameObject, normalizedName);
		if (spawnTarget != null)
		{
			trackSpawn(spawnTarget);
		}
	}

	private void trackSpawn(CountdownTarget target)
	{
		matchingSpawnCounts.merge(target.normalizedName, 1, Integer::sum);
		String key = countdownKey(SPAWN, target.normalizedName);
		if (activeCountdowns.containsKey(key))
		{
			return;
		}

		SpawnCountdownInfoBox infoBox = new SpawnCountdownInfoBox(icon, this, target.npcName, target.countdownTicks);
		activeCountdowns.put(key, infoBox);
		infoBoxManager.addInfoBox(infoBox);
	}

	private void untrackSpawn(String normalizedName)
	{
		if (normalizedName == null)
		{
			return;
		}

		int remaining = matchingSpawnCounts.getOrDefault(normalizedName, 0) - 1;
		if (remaining <= 0)
		{
			matchingSpawnCounts.remove(normalizedName);
			if (config.removeOnDespawn())
			{
				removeCountdown(SPAWN, normalizedName);
			}
			return;
		}

		matchingSpawnCounts.put(normalizedName, remaining);
	}

	private void trackNpcDeath(NPC npc, String normalizedName)
	{
		CountdownTarget target = targetsByName(config.npcDeathCountdowns()).get(normalizedName);
		if (target == null)
		{
			return;
		}

		TrackedNpcDeath death = getOrCreateTrackedDeath(npc, target.countdownTicks);
		death.setSpawnPoint(getNpcWorldPoint(npc));
		death.setDiedOnTick(client.getTickCount());
	}

	private void trackGameObjectDeath(GameObject gameObject, String normalizedName)
	{
		CountdownTarget target = targetsByName(config.npcDeathCountdowns()).get(normalizedName);
		if (target == null)
		{
			return;
		}

		TrackedNpcDeath death = new TrackedNpcDeath(target.npcName, getGameObjectSize(gameObject), target.countdownTicks);
		death.setSpawnPoint(gameObject.getWorldLocation());
		death.setDiedOnTick(client.getTickCount());
		trackedDeaths.add(death);
	}

	private TrackedNpcDeath getOrCreateTrackedDeath(NPC npc, int countdownTicks)
	{
		for (TrackedNpcDeath death : trackedDeaths)
		{
			if (death.matches(npc))
			{
				return death;
			}
		}

		TrackedNpcDeath death = new TrackedNpcDeath(npc, countdownTicks);
		trackedDeaths.add(death);
		return death;
	}

	private boolean hasDeathCountdown(String normalizedName)
	{
		return normalizedName != null && targetsByName(config.npcDeathCountdowns()).containsKey(normalizedName);
	}

	private WorldPoint getNpcWorldPoint(NPC npc)
	{
		return client.isInInstancedRegion()
			? WorldPoint.fromLocalInstance(client, npc.getLocalLocation())
			: WorldPoint.fromLocal(client, npc.getLocalLocation());
	}

	private String getGameObjectName(GameObject gameObject)
	{
		ObjectComposition composition = getGameObjectComposition(gameObject);
		return composition == null ? null : composition.getName();
	}

	private int getGameObjectSize(GameObject gameObject)
	{
		ObjectComposition composition = getGameObjectComposition(gameObject);
		return composition == null ? 1 : Math.max(composition.getSizeX(), composition.getSizeY());
	}

	private ObjectComposition getGameObjectComposition(GameObject gameObject)
	{
		ObjectComposition composition = client.getObjectDefinition(gameObject.getId());
		if (composition != null && composition.getImpostorIds() != null)
		{
			ObjectComposition impostor = composition.getImpostor();
			if (impostor != null)
			{
				composition = impostor;
			}
		}
		return composition;
	}

	private Map<String, CountdownTarget> targetsByName(String configText)
	{
		Map<String, CountdownTarget> targets = new HashMap<>();
		for (String entry : configText.split("[\\r\\n;]+"))
		{
			CountdownTarget target = parseTarget(entry);
			if (target != null)
			{
				targets.putIfAbsent(target.normalizedName, target);
			}
		}
		return targets;
	}

	private static CountdownTarget parseTarget(String entry)
	{
		int commaIndex = entry.lastIndexOf(',');
		if (commaIndex < 0)
		{
			return null;
		}

		String npcName = entry.substring(0, commaIndex).trim();
		String ticksText = entry.substring(commaIndex + 1).trim();
		if (npcName.isEmpty() || ticksText.isEmpty())
		{
			return null;
		}

		try
		{
			int countdownTicks = Integer.parseInt(ticksText);
			return countdownTicks <= 0 ? null : new CountdownTarget(npcName, normalizeName(npcName), countdownTicks);
		}
		catch (NumberFormatException ex)
		{
			return null;
		}
	}

	private static String normalizeName(String npcName)
	{
		return Text.standardize(npcName);
	}

	private void removeAllCountdowns()
	{
		for (SpawnCountdownInfoBox infoBox : activeCountdowns.values())
		{
			infoBoxManager.removeInfoBox(infoBox);
		}
		activeCountdowns.clear();
		matchingSpawnCounts.clear();
		trackedNpcs.clear();
		trackedGameObjects.clear();
		trackedDeaths.clear();
	}

	private void removeCountdown(String countdownType, String normalizedName)
	{
		SpawnCountdownInfoBox infoBox = activeCountdowns.remove(countdownKey(countdownType, normalizedName));
		if (infoBox != null)
		{
			infoBoxManager.removeInfoBox(infoBox);
		}
	}

	private static String countdownKey(String countdownType, String normalizedName)
	{
		return countdownType + ":" + normalizedName;
	}

	private static BufferedImage createIcon()
	{
		BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
		Graphics2D graphics = image.createGraphics();
		graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		graphics.setColor(new Color(40, 43, 48));
		graphics.fillOval(1, 1, 30, 30);
		graphics.setColor(new Color(241, 196, 15));
		graphics.fillOval(6, 6, 20, 20);
		graphics.setColor(new Color(40, 43, 48));
		graphics.fillOval(10, 10, 12, 12);
		graphics.setColor(Color.WHITE);
		graphics.fillRect(15, 4, 2, 8);
		graphics.fillRect(15, 16, 8, 2);
		graphics.dispose();
		return image;
	}

	private static final class CountdownTarget
	{
		private final String npcName;
		private final String normalizedName;
		private final int countdownTicks;

		private CountdownTarget(String npcName, String normalizedName, int countdownTicks)
		{
			this.npcName = npcName;
			this.normalizedName = normalizedName;
			this.countdownTicks = countdownTicks;
		}
	}
}
