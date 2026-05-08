package net.runelite.client.plugins.spawncounter;

import com.google.inject.Provides;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.ObjectComposition;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.NpcSpawned;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;
import net.runelite.client.util.Text;

@PluginDescriptor(
	name = "<html><font color=#b82584>[J] Spawn Counter",
	description = "Shows an infobox when configured NPCs spawn",
	tags = {"npc", "spawn", "timer", "counter", "infobox"}
)
public class SpawnCounterPlugin extends Plugin
{
	private static final String SPAWN = "spawn";

	private final Map<String, SpawnCountdownInfoBox> activeCountdowns = new HashMap<>();

	@Inject
	private InfoBoxManager infoBoxManager;

	@Inject
	private SpawnCounterConfig config;

	@Inject
	private Client client;

	private BufferedImage icon;

	@Override
	protected void startUp()
	{
		icon = createIcon();
	}

	@Override
	protected void shutDown()
	{
		removeAllCountdowns();
	}

	@Subscribe
	public void onNpcSpawned(NpcSpawned event)
	{
		trackNpc(event.getNpc());
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		trackGameObject(event.getGameObject());
	}

	@Subscribe
	public void onGameTick(GameTick event)
	{
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

	private void trackNpc(NPC npc)
	{
		if (npc.getName() == null)
		{
			return;
		}

		String normalizedName = normalizeName(npc.getName());
		CountdownTarget spawnTarget = targetsByName(config.npcCountdowns()).get(normalizedName);
		if (spawnTarget != null)
		{
			trackSpawn(spawnTarget);
		}
	}

	private void trackGameObject(GameObject gameObject)
	{
		String objectName = getGameObjectName(gameObject);
		if (objectName == null)
		{
			return;
		}

		String normalizedName = normalizeName(objectName);
		CountdownTarget spawnTarget = targetsByName(config.npcCountdowns()).get(normalizedName);
		if (spawnTarget != null)
		{
			trackSpawn(spawnTarget);
		}
	}

	private void trackSpawn(CountdownTarget target)
	{
		String key = countdownKey(SPAWN, target.normalizedName);
		if (activeCountdowns.containsKey(key))
		{
			return;
		}

		SpawnCountdownInfoBox infoBox = new SpawnCountdownInfoBox(icon, this, target.npcName, target.countdownTicks);
		activeCountdowns.put(key, infoBox);
		infoBoxManager.addInfoBox(infoBox);
	}

	private String getGameObjectName(GameObject gameObject)
	{
		ObjectComposition composition = getGameObjectComposition(gameObject);
		return composition == null ? null : composition.getName();
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
