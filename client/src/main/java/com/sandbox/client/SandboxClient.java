package com.sandbox.client;

import com.badlogic.gdx.Game;
import com.common.sandbox.network.packets.*;
import com.sandbox.client.screens.ScreenManager;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SandboxClient extends Game {
    private static final Logger logger = LoggerFactory.getLogger(SandboxClient.class);

    private NetworkClient networkClient;
    private ScreenManager screenManager;
    private boolean isProduction = false;

    // ==================== FILAS PARA ITENS PENDENTES ====================
    private final ConcurrentLinkedQueue<ItemSpawnPacket> pendingItemSpawns = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ItemDespawnPacket> pendingItemDespawns = new ConcurrentLinkedQueue<>();

    // ==================== FILAS PARA INVENTÁRIO PENDENTE ====================
    private final ConcurrentLinkedQueue<InventoryUpdatePacket> pendingInventoryUpdates = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<PickupResultPacket> pendingPickupResults = new ConcurrentLinkedQueue<>();

    // ==================== REFERÊNCIA PARA O RENDERER ====================
    public Object currentGameWorldRenderer;
    private Runnable onGameWorldRendererReady;

    @Override
    public void create() {
        logger.info("Starting Sandbox Client");

        networkClient = new NetworkClient("localhost", 8080);

        setupGlobalCallbacks();

        networkClient.connect();

        screenManager = new ScreenManager(this);

        if (!isProduction) {
            screenManager.showAdminSelector();
        } else {
            screenManager.showLogin(false);
        }
    }

    /**
     * Configura todos os callbacks globais do NetworkClient
     */
    private void setupGlobalCallbacks() {
        logger.info("Setting up GLOBAL callbacks on SandboxClient");

        // Callbacks de itens
        networkClient.setItemSpawnCallback(this::onItemSpawn);
        networkClient.setItemDespawnCallback(this::onItemDespawn);
        networkClient.setItemDefinitionSyncCallback(this::onItemDefinitionSync);

        // Callbacks de inventário
        networkClient.setInventoryCallback(this::onInventoryUpdate);
        networkClient.setPickupResultCallback(this::onPickupResult);

    }

    // ==================== CALLBACKS DE ITENS ====================

    public void onItemSpawn(ItemSpawnPacket packet) {
        logger.info("Global onItemSpawn called for: {}", packet.item.getDefinition().getName());

        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            ((GameWorldRenderer) currentGameWorldRenderer).onItemSpawn(packet);
        } else {
            pendingItemSpawns.add(packet);
            logger.info("Item queued (renderer not ready). Queue size: {}", pendingItemSpawns.size());
        }
    }

    private void onItemDespawn(ItemDespawnPacket packet) {
        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            ((GameWorldRenderer) currentGameWorldRenderer).onItemDespawn(packet);
        } else {
            pendingItemDespawns.add(packet);
        }
    }

    public void onItemDefinitionSync(ItemDefinitionSyncPacket packet) {
        logger.info("Received {} item definitions from server", packet.itemDefinitions.size());
        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            ((GameWorldRenderer) currentGameWorldRenderer).onItemDefinitionSync(packet);
        }
    }
    // ==================== CALLBACKS DE INVENTÁRIO ====================

    private void onInventoryUpdate(InventoryUpdatePacket packet) {
        logger.info("Global onInventoryUpdate called - Action: {}", packet.action);

        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            ((GameWorldRenderer) currentGameWorldRenderer).onInventoryUpdate(packet);
        } else {
            pendingInventoryUpdates.add(packet);
            logger.info("Inventory update queued (renderer not ready). Queue size: {}", pendingInventoryUpdates.size());
        }
    }

    private void onPickupResult(PickupResultPacket packet) {
        logger.info("Global onPickupResult called - Success: {}, Item: {}", packet.success, packet.itemName);

        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            ((GameWorldRenderer) currentGameWorldRenderer).onPickupResult(packet);
        } else {
            pendingPickupResults.add(packet);
            logger.info("Pickup result queued (renderer not ready). Queue size: {}", pendingPickupResults.size());
        }
    }

    // ==================== REGISTRO DO RENDERER ====================

    /**
     * Chamado quando o GameWorldRenderer está pronto para receber itens e inventário
     */
    public void registerGameWorldRenderer(GameWorldRenderer renderer) {
        logger.info("Registering GameWorldRenderer - Processing pending items and inventory updates");
        this.currentGameWorldRenderer = renderer;

        // Processar itens pendentes
        processPendingItemSpawns(renderer);
        processPendingItemDespawns(renderer);

        // Processar inventário pendente
        processPendingInventoryUpdates(renderer);
        processPendingPickupResults(renderer);

        if (onGameWorldRendererReady != null) {
            onGameWorldRendererReady.run();
        }
    }

    private void processPendingItemSpawns(GameWorldRenderer renderer) {
        ItemSpawnPacket spawnPacket;
        int count = 0;
        while ((spawnPacket = pendingItemSpawns.poll()) != null) {
            logger.info("Processing queued item spawn: {}", spawnPacket.item.getDefinition().getName());
            renderer.onItemSpawn(spawnPacket);
            count++;
        }
        if (count > 0) {
            logger.info("Processed {} queued item spawns", count);
        }
    }

    private void processPendingItemDespawns(GameWorldRenderer renderer) {
        ItemDespawnPacket despawnPacket;
        int count = 0;
        while ((despawnPacket = pendingItemDespawns.poll()) != null) {
            renderer.onItemDespawn(despawnPacket);
            count++;
        }
        if (count > 0) {
            logger.info("Processed {} queued item despawns", count);
        }
    }

    private void processPendingInventoryUpdates(GameWorldRenderer renderer) {
        InventoryUpdatePacket invPacket;
        int count = 0;
        while ((invPacket = pendingInventoryUpdates.poll()) != null) {
            logger.info("Processing queued inventory update - Action: {}", invPacket.action);
            renderer.onInventoryUpdate(invPacket);
            count++;
        }
        if (count > 0) {
            logger.info("Processed {} queued inventory updates", count);
        }
    }

    private void processPendingPickupResults(GameWorldRenderer renderer) {
        PickupResultPacket resultPacket;
        int count = 0;
        while ((resultPacket = pendingPickupResults.poll()) != null) {
            logger.info("Processing queued pickup result - Item: {}", resultPacket.itemName);
            renderer.onPickupResult(resultPacket);
            count++;
        }
        if (count > 0) {
            logger.info("Processed {} queued pickup results", count);
        }
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    public void setNetworkClient(NetworkClient networkClient) {
        if (this.networkClient != null) {
            this.networkClient.disconnect();
        }
        this.networkClient = networkClient;
        setupGlobalCallbacks();
    }

    public void setOnGameWorldRendererReady(Runnable callback) {
        this.onGameWorldRendererReady = callback;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public ScreenManager getScreenManager() {
        return screenManager;
    }

    public void startGame(Player player, boolean adminMode, Map<String, Player> nearbyPlayers) {
        screenManager.showGame(player, adminMode, nearbyPlayers);
    }

    public void openMapEditor() {
        screenManager.showMapEditor();
    }

    public boolean isProduction() {
        return isProduction;
    }

    // ==================== MÉTODOS DE UTILIDADE PARA DEBUG ====================

    public void setProduction(boolean production) {
        this.isProduction = production;
    }

    public int getPendingItemSpawnCount() {
        return pendingItemSpawns.size();
    }

    public int getPendingItemDespawnCount() {
        return pendingItemDespawns.size();
    }

    public int getPendingInventoryUpdateCount() {
        return pendingInventoryUpdates.size();
    }

    public int getPendingPickupResultCount() {
        return pendingPickupResults.size();
    }

    public void clearPendingQueues() {
        pendingItemSpawns.clear();
        pendingItemDespawns.clear();
        pendingInventoryUpdates.clear();
        pendingPickupResults.clear();
        logger.info("All pending queues cleared");
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        logger.info("Disposing SandboxClient...");
        if (networkClient != null) {
            networkClient.disconnect();
        }
        clearPendingQueues();
        super.dispose();
        logger.info("SandboxClient disposed");
    }
}