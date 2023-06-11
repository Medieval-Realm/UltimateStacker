package com.craftaro.ultimatestacker.stackable.spawner;

import com.craftaro.ultimatestacker.UltimateStacker;
import com.craftaro.ultimatestacker.api.UltimateStackerAPI;
import com.craftaro.ultimatestacker.api.stack.spawner.SpawnerStack;
import com.craftaro.ultimatestacker.settings.Settings;
import com.craftaro.ultimatestacker.utils.Methods;
import com.craftaro.core.compatibility.CompatibleMaterial;
import com.craftaro.core.database.Data;
import com.craftaro.core.database.SerializedLocation;
import com.craftaro.core.nms.world.SpawnedEntity;
import com.craftaro.core.world.SSpawner;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.entity.EntityType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class SpawnerStackImpl extends SSpawner implements SpawnerStack {

    // This is the unique identifier for this spawner.
    // It is reset on every plugin load.
    // Used for holograms.
    private final UUID uniqueId = UUID.randomUUID();
    
    private int id;

    private int amount;

    private static final UltimateStacker plugin = UltimateStacker.getInstance();

    public SpawnerStackImpl(Location location, int amount) {
        super(location);
        this.amount = amount;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public boolean isValid() {
        return CompatibleMaterial.getMaterial(location.getBlock()) == CompatibleMaterial.SPAWNER;
    }

    @Override
    public void setAmount(int amount) {
        this.amount = amount;
        plugin.getPluginDataManager().save(this);
    }

    @Override
    public void add(int amount) {
        this.amount += amount;
    }

    @Override
    public void take(int amount) {
        this.amount -= amount;
    }

    public int calculateSpawnCount(EntityType type) {
        if (!UltimateStacker.getInstance().getMobFile().getBoolean("Mobs." + type.name() + ".Enabled")) {
            return 0;
        }

        Random random = new Random();
        int count = 0;
        for (int i = 0; i < getAmount(); i++) {
            count += random.nextInt(3) + 1;
        }
        return count;
    }

    public int spawn(int amountToSpawn, EntityType... types) {
        return super.spawn(amountToSpawn, types);
    }

    public int spawn(int amountToSpawn, String particle, Set<CompatibleMaterial> canSpawnOn, SpawnedEntity spawned, EntityType... types) {
        return super.spawn(amountToSpawn, particle, canSpawnOn, spawned, types);
    }

    public int getId() {
        return this.id;
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("amount", amount);
        map.putAll(new SerializedLocation(location).asMap());
        return map;
    }

    @Override
    public Data deserialize(Map<String, Object> map) {
        this.id = (int) map.get("id");
        this.amount = (int) map.get("amount");
        this.initFromData(map);
        return this;
    }

    @Override
    public String getTableName() {
        return "spawners";
    }

    public void setId(int id) {
        this.id = id;
    }

    public Location getLocation() {
        return location.clone();
    }

    @Override
    public String getHologramName() {
        if (!(location.getBlock().getState() instanceof CreatureSpawner)) {
            UltimateStackerAPI.getSpawnerStackManager().removeSpawner(location);
            return null;
        }
        CreatureSpawner creatureSpawner = (CreatureSpawner) location.getBlock().getState();
        return Methods.compileSpawnerName(creatureSpawner.getSpawnedType(), amount);
    }

    @Override
    public boolean areHologramsEnabled() {
        return Settings.SPAWNER_HOLOGRAMS.getBoolean();
    }

    public int getX() {
        return location.getBlockX();
    }

    public int getY() {
        return location.getBlockY();
    }

    public int getZ() {
        return location.getBlockZ();
    }

    public World getWorld() {
        return location.getWorld();
    }

    @Override
    public String getHologramId() {
        return "UltimateStacker-" + uniqueId;
    }

    @Override
    public String toString() {
        return "SpawnerStackImpl:{"
                + "Amount:\"" + amount + "\","
                + "Location:{"
                + "World:\"" + location.getWorld().getName() + "\","
                + "X:" + location.getBlockX() + ","
                + "Y:" + location.getBlockY() + ","
                + "Z:" + location.getBlockZ()
                + "}"
                + "}";
    }
}