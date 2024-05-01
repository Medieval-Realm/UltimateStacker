package com.craftaro.ultimatestacker.listeners;

import com.craftaro.core.compatibility.CompatibleHand;
import com.craftaro.core.compatibility.ServerVersion;
import com.craftaro.core.nms.Nms;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTCompound;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTEntity;
import com.craftaro.core.third_party.de.tr7zw.nbtapi.NBTItem;
import com.craftaro.core.utils.EntityUtils;
import com.craftaro.ultimatestacker.UltimateStacker;
import com.craftaro.ultimatestacker.api.UltimateStackerApi;
import com.craftaro.ultimatestacker.api.stack.spawner.SpawnerStack;
import com.craftaro.ultimatestacker.api.stack.spawner.SpawnerStackManager;
import com.craftaro.ultimatestacker.settings.Settings;
import com.craftaro.ultimatestacker.stackable.spawner.SpawnerStackImpl;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.material.SpawnEgg;
import org.bukkit.metadata.FixedMetadataValue;

public class SpawnerListeners implements Listener {

    private final UltimateStacker plugin;

    private static final boolean mcmmo = Bukkit.getPluginManager().isPluginEnabled("mcMMO");

    public SpawnerListeners(UltimateStacker plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onSpawn(SpawnerSpawnEvent event) {
        if (plugin.getStackingTask() == null) return; //Don't spam error when reloading the plugin
        if (!plugin.spawnersEnabled()
                || plugin.getStackingTask().isWorldDisabled(event.getLocation().getWorld())) return;

        SpawnerStackManager spawnerStackManager = UltimateStackerApi.getSpawnerStackManager();
        if (!spawnerStackManager.isSpawner(event.getSpawner().getLocation())) return;

        Entity entity = event.getEntity();
        if (entity.getType() == EntityType.FIREWORK) return;
        if (entity.getVehicle() != null) {
            entity.getVehicle().remove();
        }

        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_11)) {
            if (entity.getPassengers().size() != 0) {
                for (Entity e : entity.getPassengers()) {
                    e.remove();
                }
            }
        }

        Location location = event.getSpawner().getLocation();

        SpawnerStack spawnerStack = spawnerStackManager.getSpawner(location);

        int amountToSpawn = Settings.STACK_ENTITIES.getBoolean() ? spawnerStack.calculateSpawnCount(entity.getType(), false) : 1;
        if (amountToSpawn <= 0) return;
        entity.remove();

        spawnerStack.spawn(amountToSpawn, "EXPLOSION_NORMAL", null, (e) -> {
            if (Settings.NO_AI.getBoolean()) {
                EntityUtils.setUnaware(e);
            }

            if (mcmmo) {
                entity.setMetadata("mcMMO: Spawned Entity", new FixedMetadataValue(plugin, true));
            }

            UltimateStacker.getInstance().getEntityStackManager().createStackedEntity(e, amountToSpawn);
            return true;
        }, event.getEntityType());
    }

    @EventHandler
    public void PlayerInteractEventEgg(PlayerInteractEvent event) {
        if (!plugin.spawnersEnabled()
                || !event.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            return;

        if (event.getItem() == null)
            return;

        Material itemType = event.getItem().getType();
        Block block = event.getClickedBlock();
        Player player = event.getPlayer();

        if (block == null || itemType == Material.AIR) return;

        if (block.getType() != (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? Material.SPAWNER : Material.valueOf("MOB_SPAWNER"))
                || !itemType.toString().contains(ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13) ? "SPAWN_EGG" : "MONSTER_EGG"))
            return;

        event.setCancelled(true);

        if (!Settings.EGGS_CONVERT_SPAWNERS.getBoolean()
                || (event.getItem().hasItemMeta() && event.getItem().getItemMeta().hasDisplayName()
                && !new NBTItem(event.getItem()).hasKey("UC"))) {
            return;
        }

        SpawnerStackManager manager = UltimateStackerApi.getSpawnerStackManager();

        SpawnerStack spawner = manager.isSpawner(block.getLocation())
                ? manager.getSpawner(block) : manager.addSpawner(new SpawnerStackImpl(block.getLocation(), 1));

        int stackSize = spawner.getAmount();
        int amt = player.getInventory().getItemInHand().getAmount();

        EntityType entityType;
        if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_13))
            entityType = EntityType.valueOf(itemType.name().replace("_SPAWN_EGG", "")
                    .replace("MOOSHROOM", "MUSHROOM_COW")
                    .replace("ZOMBIE_PIGMAN", "PIG_ZOMBIE"));
        else if (ServerVersion.isServerVersionAtLeast(ServerVersion.V1_12)) {
            Nms.getImplementations().getNbt().of(event.getItem()).toString();
            NBTCompound entityTag = new NBTItem(event.getItem()).getCompound("EntityTag");
            if (entityTag != null) {
                entityType = EntityType.fromName(entityTag.getString("id"));
            } else {
                entityType = EntityType.fromName(event.getItem().getItemMeta().getDisplayName().replace(" Spawn Egg", ""));
            }
        } else {
            entityType = ((SpawnEgg) event.getItem().getData()).getSpawnedType();
        }

        if (!player.hasPermission("ultimatestacker.egg." + entityType.name())) {
            event.setCancelled(true);
            return;
        }

        if (amt < stackSize) {
            plugin.getLocale().getMessage("event.egg.needmore")
                    .processPlaceholder("amount", stackSize).sendPrefixedMessage(player);
            event.setCancelled(true);
            return;
        }


        CreatureSpawner creatureSpawner = (CreatureSpawner) block.getState();

        if (entityType == creatureSpawner.getSpawnedType()) {
            plugin.getLocale().getMessage("event.egg.sametype")
                    .processPlaceholder("type", entityType.name()).sendPrefixedMessage(player);
            return;
        }

        creatureSpawner.setSpawnedType(entityType);
        creatureSpawner.update();

        plugin.updateHologram(spawner);
        if (player.getGameMode() != GameMode.CREATIVE)
            CompatibleHand.getHand(event).takeItem(player, stackSize);
    }
}
