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
        // ==================== ESPADAS ====================

        // Espada Simples
        ItemDefinition sword = new ItemDefinition();
        sword.setId("simple_sword");
        sword.setName("Espada Simples");
        sword.setCategory("weapon");
        sword.setSpritesheet("itens/spritesheet_itens.png");
        sword.setTileX(1);
        sword.setTileY(0);
        sword.setDamage(10);
        sword.setAttackId("melee_sword");
        sword.setAttackAnimation("sword_slash");
        sword.setAttackCooldown(1.0f);
        sword.setProjectileAnimationId("slash");
        sword.setHitboxDuration(0.5f);
        sword.setRanged(false);
        itemDefinitions.put(sword.getId(), sword);

        // Espada de Ferro
        ItemDefinition ironSword = new ItemDefinition();
        ironSword.setId("iron_sword");
        ironSword.setName("Espada de Ferro");
        ironSword.setCategory("weapon");
        ironSword.setSpritesheet("itens/spritesheet_itens.png");
        ironSword.setTileX(1);
        ironSword.setTileY(0);
        ironSword.setDamage(18);
        ironSword.setAttackId("melee_sword");
        ironSword.setAttackAnimation("sword_slash");
        ironSword.setAttackCooldown(1.25f);
        ironSword.setProjectileAnimationId("slash");
        ironSword.setRanged(false);
        itemDefinitions.put(ironSword.getId(), ironSword);

        // Adaga
        ItemDefinition dagger = new ItemDefinition();
        dagger.setId("dagger");
        dagger.setName("Adaga");
        dagger.setCategory("weapon");
        dagger.setSpritesheet("itens/spritesheet_itens.png");
        dagger.setTileX(2);
        dagger.setTileY(0);
        dagger.setDamage(6);
        dagger.setAttackId("melee_dagger");
        dagger.setAttackAnimation("dagger_stab");
        dagger.setAttackCooldown(0.67f);
        dagger.setProjectileAnimationId("stab");
        dagger.setRanged(false);
        itemDefinitions.put(dagger.getId(), dagger);

        // Machado Simples
        ItemDefinition axe = new ItemDefinition();
        axe.setId("simple_axe");
        axe.setName("Machado Simples");
        axe.setCategory("weapon");
        axe.setSpritesheet("itens/spritesheet_itens.png");
        axe.setTileX(3);
        axe.setTileY(0);
        axe.setDamage(14);
        axe.setAttackId("melee_axe");
        axe.setAttackAnimation("sword_slash");
        axe.setAttackCooldown(1.43f);
        axe.setProjectileAnimationId("slash");
        axe.setRanged(false);
        itemDefinitions.put(axe.getId(), axe);

        // ==================== ARCOS ====================

        // Arco Simples
        ItemDefinition simpleBow = new ItemDefinition();
        simpleBow.setId("simple_bow");
        simpleBow.setName("Arco Simples");
        simpleBow.setCategory("weapon");
        simpleBow.setSpritesheet("itens/spritesheet_itens.png");
        simpleBow.setTileX(0);
        simpleBow.setTileY(1);
        simpleBow.setDamage(8);
        simpleBow.setAttackId("ranged_bow");
        simpleBow.setAttackAnimation("bow_shoot");
        simpleBow.setAttackCooldown(1.25f);
        simpleBow.setRanged(true);
        simpleBow.setProjectileId("arrow");
        simpleBow.setProjectileSpeed(600f);
        simpleBow.setProjectileRange(400f);
        simpleBow.setProjectileAnimationId("arrow");
        itemDefinitions.put(simpleBow.getId(), simpleBow);

        // Arco Longo
        ItemDefinition longBow = new ItemDefinition();
        longBow.setId("long_bow");
        longBow.setName("Arco Longo");
        longBow.setCategory("weapon");
        longBow.setSpritesheet("itens/spritesheet_itens.png");
        longBow.setTileX(1);
        longBow.setTileY(1);
        longBow.setDamage(15);
        longBow.setAttackId("ranged_bow");
        longBow.setAttackAnimation("bow_shoot");
        longBow.setAttackCooldown(1.67f);
        longBow.setRanged(true);
        longBow.setProjectileId("arrow");
        longBow.setProjectileSpeed(900f);
        longBow.setProjectileRange(550f);
        longBow.setProjectileAnimationId("arrow");
        itemDefinitions.put(longBow.getId(), longBow);

        // Arco Rápido
        ItemDefinition quickBow = new ItemDefinition();
        quickBow.setId("quick_bow");
        quickBow.setName("Arco Rápido");
        quickBow.setCategory("weapon");
        quickBow.setSpritesheet("itens/spritesheet_itens.png");
        quickBow.setTileX(2);
        quickBow.setTileY(1);
        quickBow.setDamage(6);
        quickBow.setAttackId("ranged_bow");
        quickBow.setAttackAnimation("bow_shoot");
        quickBow.setAttackCooldown(0.77f);
        quickBow.setRanged(true);
        quickBow.setProjectileId("arrow");
        quickBow.setProjectileSpeed(450f);
        quickBow.setProjectileRange(300f);
        quickBow.setProjectileAnimationId("arrow");
        itemDefinitions.put(quickBow.getId(), quickBow);

        // ==================== ITENS CONSUMÍVEIS ====================

        ItemDefinition apple = new ItemDefinition();
        apple.setId("apple");
        apple.setName("Maçã");
        apple.setCategory("consumable");
        apple.setSpritesheet("itens/spritesheet_itens.png");
        apple.setTileX(0);
        apple.setTileY(0);
        apple.setHealAmount(25);
        itemDefinitions.put(apple.getId(), apple);

        ItemDefinition healthPotion = new ItemDefinition();
        healthPotion.setId("health_potion");
        healthPotion.setName("Poção de Vida");
        healthPotion.setCategory("consumable");
        healthPotion.setSpritesheet("itens/spritesheet_itens.png");
        healthPotion.setTileX(3);
        healthPotion.setTileY(1);
        healthPotion.setHealAmount(50);
        itemDefinitions.put(healthPotion.getId(), healthPotion);

        // LOG MELHORADO: Percorrer todos os itens e printar informações completas
        printAllItemDefinitions();
    }

    private void printAllItemDefinitions() {
        logger.info("========================================");
        logger.info("        ITEM DEFINITIONS LOADED         ");
        logger.info("========================================");

        for (ItemDefinition def : itemDefinitions.values()) {
            StringBuilder sb = new StringBuilder();
            sb.append(String.format("📦 %s (ID: %s)", def.getName(), def.getId()));
            sb.append(String.format(" | Category: %s", def.getCategory()));

            if ("weapon".equals(def.getCategory())) {
                sb.append(String.format(" | Damage: %d", def.getDamage()));
                sb.append(String.format(" | Cooldown: %.2fs", def.getAttackCooldown()));
                sb.append(String.format(" | Hitbox: %.2fs", def.getHitboxDuration()));
                if (def.isRanged()) {
                    sb.append(String.format(" | Ranged: ✓ | Speed: %.0f | Range: %.0f",
                            def.getProjectileSpeed(), def.getProjectileRange()));
                    sb.append(String.format(" | Anim: %s", def.getProjectileAnimationId()));
                } else {
                    sb.append(String.format(" | Melee: ✓ | Anim: %s", def.getProjectileAnimationId()));
                }
            } else if ("consumable".equals(def.getCategory())) {
                sb.append(String.format(" | Heal: %d", def.getHealAmount()));
            }

            sb.append(String.format(" | Sprite: (%d,%d) %s", def.getTileX(), def.getTileY(), def.getSpritesheet()));

            logger.info(sb.toString());
        }

        logger.info("========================================");
        logger.info("Total items loaded: {}", itemDefinitions.size());
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

        // Espadas
        spawnItem("simple_sword", 400, 300, 60);
        spawnItem("iron_sword", 550, 320, 90);

        // Adaga
        spawnItem("dagger", 350, 400, 45);

        // Arcos
        spawnItem("simple_bow", 500, 350, 60);
        spawnItem("long_bow", 600, 380, 90);
        spawnItem("quick_bow", 450, 420, 60);

        // Machado
        spawnItem("simple_axe", 480, 280, 75);

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

    public Map<String, ItemDefinition> getAllItemDefinitions() {
        return new HashMap<>(itemDefinitions);
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