package com.sandbox.server;

import com.common.sandbox.model.ItemDefinition;
import com.common.sandbox.model.GroundItem;
import com.common.sandbox.network.packets.ItemSpawnPacket;
import com.common.sandbox.network.packets.ItemDespawnPacket;
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

    private final Map<String, ItemDefinition> itemDefinitions;
    private final Map<String, GroundItem> groundItems;
    private final Map<String, Set<String>> chunkItems;

    private final ScheduledExecutorService scheduler;

    private boolean worldItemsSpawned = false;

    private ItemManager() {
        this.itemDefinitions = new ConcurrentHashMap<>();
        this.groundItems = new ConcurrentHashMap<>();
        this.chunkItems = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();

        loadItemDefinitions();
        startDespawnTask();
        // NÃO chamar spawnInitialItems aqui
    }

    public static synchronized ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    private void loadItemDefinitions() {
        // Espada Simples
        ItemDefinition sword = new ItemDefinition();
        sword.setId("simple_sword");
        sword.setName("Espada Simples");
        sword.setCategory("weapon");
        sword.setSpritesheet("itens/spritesheet_itens.png");
        sword.setTileX(1);
        sword.setTileY(0);
        sword.setDamage(10);
        itemDefinitions.put(sword.getId(), sword);
        logger.info("Loaded item definition: {}", sword.getName());

        // Maca (fruta que cura)
        ItemDefinition apple = new ItemDefinition();
        apple.setId("apple");
        apple.setName("Maca");
        apple.setCategory("consumable");
        apple.setSpritesheet("itens/spritesheet_itens.png");
        apple.setTileX(0);
        apple.setTileY(0);
        apple.setHealAmount(25);
        itemDefinitions.put(apple.getId(), apple);
        logger.info("Loaded item definition: {}", apple.getName());

        // Pocao de Vida
        ItemDefinition healthPotion = new ItemDefinition();
        healthPotion.setId("health_potion");
        healthPotion.setName("Pocao de Vida");
        healthPotion.setCategory("consumable");
        healthPotion.setSpritesheet("itens/spritesheet_itens.png");
        healthPotion.setTileX(2);
        healthPotion.setTileY(0);
        healthPotion.setHealAmount(50);
        itemDefinitions.put(healthPotion.getId(), healthPotion);
        logger.info("Loaded item definition: {}", healthPotion.getName());
    }

    public void spawnWorldItems() {
        if (worldItemsSpawned) {
            logger.warn("World items already spawned! Skipping...");
            return;
        }

        logger.info("=== SPAWNING WORLD ITEMS ===");

        // Limpar itens existentes
        groundItems.clear();
        chunkItems.clear();

        // Spawnar itens com diferentes tempos de vida
        spawnItem("simple_sword", 400, 300, 60);      // 60 segundos
        spawnItem("apple", 450, 350, 45);              // 45 segundos
        spawnItem("simple_sword", 500, 400, 90);       // 90 segundos
        spawnItem("health_potion", 350, 250, 30);      // 30 segundos

        worldItemsSpawned = true;
        logger.info("World items spawned: {} items", groundItems.size());
        printAllItems();
    }

    public void spawnItem(String itemId, float x, float y, int despawnSeconds) {
        ItemDefinition def = itemDefinitions.get(itemId);
        if (def == null) {
            logger.warn("Tried to spawn unknown item: {}", itemId);
            return;
        }

        String instanceId = UUID.randomUUID().toString();
        GroundItem item = new GroundItem(instanceId, def, x, y, despawnSeconds);

        groundItems.put(instanceId, item);
        addToChunkIndex(item);

        logger.info("ITEM SPAWNED - ID: {}, Name: {}, Category: {}, Pos: ({}, {})",
                instanceId.substring(0, 8), def.getName(), def.getCategory(), x, y);

        ItemSpawnPacket packet = new ItemSpawnPacket(item);
        GameServerHandler.broadcastToAll(packet);
    }

    private void despawnItem(String instanceId) {
        GroundItem item = groundItems.remove(instanceId);
        if (item != null) {
            removeFromChunkIndex(item);

            ItemDespawnPacket packet = new ItemDespawnPacket(instanceId);
            GameServerHandler.broadcastToAll(packet);

            logger.info("DESPAWNED - Item: {} [{}] after {}s",
                    item.getDefinition().getName(),
                    instanceId.substring(0, 8),
                    item.getDespawnSeconds());
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

    public List<String> getAllItemIds() {
        return new ArrayList<>(itemDefinitions.keySet());
    }

    public Collection<GroundItem> getAllItems() {
        return new ArrayList<>(groundItems.values());
    }

    public void printAllItems() {
        logger.info("=== ALL ITEMS IN MANAGER ===");
        logger.info("Total items: {}", groundItems.size());
        if (groundItems.isEmpty()) {
            logger.info("  No items currently in manager");
        } else {
            for (GroundItem item : groundItems.values()) {
                long timeLeft = (item.getSpawnTime() + (item.getDespawnSeconds() * 1000L)) - System.currentTimeMillis();
                logger.info("  Item: {} [{}] at ({}, {}) - Despawn in {}s",
                        item.getDefinition().getName(),
                        item.getInstanceId().substring(0, 8),
                        item.getX(),
                        item.getY(),
                        timeLeft / 1000);
            }
        }
        logger.info("============================");
    }

    public int getItemCount() {
        return groundItems.size();
    }

    public void resendAllItemsToPlayer(ChannelHandlerContext ctx) {
        logger.info("Resending all items to player - Total items: {}", groundItems.size());
        for (GroundItem item : groundItems.values()) {
            ctx.writeAndFlush(new ItemSpawnPacket(item));
        }
    }

    public GroundItem removeItem(String instanceId) {
        GroundItem item = groundItems.remove(instanceId);
        if (item != null) {
            removeFromChunkIndex(item);
            logger.info("Item removed: {} [{}]", item.getDefinition().getName(), instanceId.substring(0, 8));
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

    public ItemDefinition getItemDefinition(String itemId) {
        return itemDefinitions.get(itemId);
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