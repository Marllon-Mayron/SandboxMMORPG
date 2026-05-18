package com.sandbox.server;

import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.item.GroundItem;
import com.common.sandbox.network.packets.inventory.ItemSpawnPacket;
import com.common.sandbox.network.packets.inventory.ItemDespawnPacket;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ItemManager {
    private static final Logger logger = LoggerFactory.getLogger(ItemManager.class);
    private static ItemManager instance;

    // Definições de itens (organizadas por tipo)
    private final Map<String, ItemDefinition> weapons;
    private final Map<String, ItemDefinition> armors;
    private final Map<String, ItemDefinition> consumables;
    private final Map<String, ItemDefinition> allItems;
    private final Map<String, ItemDefinition> accessories;

    // Itens no chão do mundo
    private final Map<String, GroundItem> groundItems;
    private final Map<String, Set<String>> chunkItems;

    private final ScheduledExecutorService scheduler;
    private boolean worldItemsSpawned = false;

    private ItemManager() {
        this.weapons = new ConcurrentHashMap<>();
        this.armors = new ConcurrentHashMap<>();
        this.consumables = new ConcurrentHashMap<>();
        this.allItems = new ConcurrentHashMap<>();
        this.groundItems = new ConcurrentHashMap<>();
        this.accessories = new ConcurrentHashMap<>();
        this.chunkItems = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadItemDefinitions();
        startDespawnTask();
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    private void loadItemDefinitions() {
        ItemDataLoader loader = new ItemDataLoader();
        loader.loadAllItems();

        weapons.putAll(loader.getWeapons());
        armors.putAll(loader.getArmors());
        consumables.putAll(loader.getConsumables());
        accessories.putAll(loader.getAccessories());
        allItems.putAll(loader.getAllItems());

        printSummary();
    }

    private void printSummary() {
        logger.info("========================================");
        logger.info("        ITEMS LOADED SUCCESSFULLY       ");
        logger.info("========================================");
        logger.info("Weapons:     {}", weapons.size());
        logger.info("Armors:      {}", armors.size());
        logger.info("Consumables: {}", consumables.size());
        logger.info("Accessories: {}", accessories.size());
        logger.info("----------------------------------------");
        logger.info("Total:       {}", allItems.size());
        logger.info("========================================");
    }

    public void spawnWorldItems() {
        if (worldItemsSpawned) {
            logger.warn("World items already spawned! Skipping...");
            return;
        }

        logger.info("=== SPAWNING WORLD ITEMS ===");
        groundItems.clear();
        chunkItems.clear();

        // Armas
        spawnItem("simple_sword", 400, 300, 60);
        spawnItem("iron_sword", 550, 320, 90);
        spawnItem("dagger", 350, 400, 45);
        spawnItem("simple_axe", 480, 280, 75);

        // Arcos
        spawnItem("simple_bow", 500, 350, 60);
        spawnItem("long_bow", 600, 380, 90);
        spawnItem("quick_bow", 450, 420, 60);

        // Consumíveis
        spawnItem("apple", 430, 330, 45);
        spawnItem("health_potion", 380, 270, 30);
        spawnItem("apple", 520, 370, 45);
        spawnItem("health_potion", 560, 310, 30);

        worldItemsSpawned = true;
        logger.info("World items spawned: {} items", groundItems.size());
        printAllItems();
    }

    public void spawnItem(String itemId, float x, float y, int despawnSeconds) {
        ItemDefinition def = allItems.get(itemId);
        if (def == null) {
            logger.warn("Tried to spawn unknown item: {}", itemId);
            return;
        }

        String instanceId = UUID.randomUUID().toString();
        GroundItem item = new GroundItem(instanceId, def, x, y, despawnSeconds);

        groundItems.put(instanceId, item);
        addToChunkIndex(item);

        logger.info("ITEM SPAWNED - {}, Category: {}, Pos: ({}, {})",
                def.getName(), def.getCategory(), x, y);

        ItemSpawnPacket packet = new ItemSpawnPacket(item);
        GameServerHandler.broadcastToAll(packet);
    }

    private void despawnItem(String instanceId) {
        GroundItem item = groundItems.remove(instanceId);
        if (item != null) {
            removeFromChunkIndex(item);
            ItemDespawnPacket packet = new ItemDespawnPacket(instanceId);
            GameServerHandler.broadcastToAll(packet);
            logger.debug("DESPAWNED - {}", item.getDefinition().getName());
        }
    }

    private void startDespawnTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                List<String> expired = new ArrayList<>();
                for (GroundItem item : groundItems.values()) {
                    if (item.isExpired()) {
                        expired.add(item.getInstanceId());
                    }
                }
                for (String id : expired) {
                    despawnItem(id);
                }
            } catch (Exception e) {
                logger.error("Error in despawn task", e);
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    private String getChunkKey(float x, float y) {
        int chunkSize = 32;
        int tileSize = 64;
        int chunkX = (int) Math.floor(x / (chunkSize * tileSize));
        int chunkY = (int) Math.floor(y / (chunkSize * tileSize));
        return chunkX + ":" + chunkY;
    }

    private void addToChunkIndex(GroundItem item) {
        String chunkKey = getChunkKey(item.getX(), item.getY());
        chunkItems.computeIfAbsent(chunkKey, k -> ConcurrentHashMap.newKeySet()).add(item.getInstanceId());
    }

    private void removeFromChunkIndex(GroundItem item) {
        String chunkKey = getChunkKey(item.getX(), item.getY());
        Set<String> items = chunkItems.get(chunkKey);
        if (items != null) {
            items.remove(item.getInstanceId());
            if (items.isEmpty()) {
                chunkItems.remove(chunkKey);
            }
        }
    }

    // ==================== GETTERS ====================

    public List<String> getAllItemIds() {
        return new ArrayList<>(allItems.keySet());
    }

    public Collection<GroundItem> getAllGroundItems() {
        return new ArrayList<>(groundItems.values());
    }

    public Map<String, ItemDefinition> getAllItemDefinitions() {
        return new HashMap<>(allItems);
    }

    public Map<String, ItemDefinition> getWeapons() {
        return new HashMap<>(weapons);
    }

    public Map<String, ItemDefinition> getArmors() {
        return new HashMap<>(armors);
    }

    public Map<String, ItemDefinition> getConsumables() {
        return new HashMap<>(consumables);
    }

    public Map<String, ItemDefinition> getAccessories() {
        return new HashMap<>(accessories);
    }

    public ItemDefinition getItemDefinition(String itemId) {
        return allItems.get(itemId);
    }

    public GroundItem getGroundItem(String instanceId) {
        return groundItems.get(instanceId);
    }

    public GroundItem removeGroundItem(String instanceId) {
        GroundItem item = groundItems.remove(instanceId);
        if (item != null) {
            removeFromChunkIndex(item);
            logger.debug("Item removed from ground: {}", item.getDefinition().getName());
        }
        return item;
    }

    public void respawnItem(GroundItem item) {
        String newInstanceId = UUID.randomUUID().toString();
        GroundItem newItem = new GroundItem(newInstanceId, item.getDefinition(),
                item.getX(), item.getY(), item.getDespawnSeconds());
        groundItems.put(newInstanceId, newItem);
        addToChunkIndex(newItem);

        ItemSpawnPacket packet = new ItemSpawnPacket(newItem);
        GameServerHandler.broadcastToAll(packet);

        logger.info("Item respawned: {} at ({}, {})",
                newItem.getDefinition().getName(), newItem.getX(), newItem.getY());
    }

    public int getGroundItemCount() {
        return groundItems.size();
    }

    public void printAllItems() {
        logger.info("=== GROUND ITEMS ({} items) ===", groundItems.size());
        for (GroundItem item : groundItems.values()) {
            long timeLeft = (item.getSpawnTime() + (item.getDespawnSeconds() * 1000L)) - System.currentTimeMillis();
            logger.info("  {} [{}] at ({}, {}) - Despawn in {}s",
                    item.getDefinition().getName(),
                    item.getInstanceId().substring(0, 8),
                    item.getX(), item.getY(),
                    timeLeft / 1000);
        }
    }

    public void resendAllItemsToPlayer(ChannelHandlerContext ctx) {
        logger.info("Resending all ground items to player - Total: {}", groundItems.size());
        for (GroundItem item : groundItems.values()) {
            ctx.writeAndFlush(new ItemSpawnPacket(item));
        }
    }

    public void shutdown() {
        logger.info("Shutting down ItemManager...");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        groundItems.clear();
        chunkItems.clear();
        logger.info("ItemManager shut down");
    }
}