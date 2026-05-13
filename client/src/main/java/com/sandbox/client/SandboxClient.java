package com.sandbox.client;

import com.badlogic.gdx.Game;
import com.sandbox.client.screens.ScreenManager;
import com.common.sandbox.model.Player;
import com.common.sandbox.network.packets.ItemSpawnPacket;
import com.common.sandbox.network.packets.ItemDespawnPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SandboxClient extends Game {
    private static final Logger logger = LoggerFactory.getLogger(SandboxClient.class);

    private NetworkClient networkClient;
    private ScreenManager screenManager;
    private boolean isProduction = false;

    // ⭐ Fila para itens recebidos antes do GameWorldRenderer estar pronto
    private final ConcurrentLinkedQueue<ItemSpawnPacket> pendingItemSpawns = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ItemDespawnPacket> pendingItemDespawns = new ConcurrentLinkedQueue<>();

    // ⭐ Callback que será chamado quando o GameWorldRenderer estiver pronto
    private Runnable onGameWorldRendererReady;

    // ⭐ Referência para o GameWorldRenderer atual
    private Object currentGameWorldRenderer;

    @Override
    public void create() {
        logger.info("Starting Sandbox Client");

        networkClient = new NetworkClient("localhost", 8080);

        // ⭐⭐⭐ CONFIGURAR CALLBACKS DE ITENS ANTES DE CONECTAR ⭐⭐⭐
        setupGlobalItemCallbacks();

        networkClient.connect();

        screenManager = new ScreenManager(this);

        // Show admin mode selector only in development
        if (!isProduction) {
            screenManager.showAdminSelector();
        } else {
            screenManager.showLogin(false);
        }
    }

    private void setupGlobalItemCallbacks() {
        logger.info("Setting up GLOBAL item callbacks on SandboxClient");

        networkClient.setItemSpawnCallback(this::onItemSpawn);
        networkClient.setItemDespawnCallback(this::onItemDespawn);
    }

    private void onItemSpawn(ItemSpawnPacket packet) {
        logger.info("Global onItemSpawn called for: {}", packet.item.getDefinition().getName());

        if (currentGameWorldRenderer instanceof GameWorldRenderer) {
            // Renderer já existe, passa diretamente
            ((GameWorldRenderer) currentGameWorldRenderer).onItemSpawn(packet);
        } else {
            // Renderer ainda não existe, enfileira
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

    /**
     * Chamado quando o GameWorldRenderer está pronto para receber itens
     */
    public void registerGameWorldRenderer(GameWorldRenderer renderer) {
        logger.info("Registering GameWorldRenderer - Processing {} pending items", pendingItemSpawns.size());
        this.currentGameWorldRenderer = renderer;

        // Processar itens pendentes
        ItemSpawnPacket spawnPacket;
        while ((spawnPacket = pendingItemSpawns.poll()) != null) {
            logger.info("Processing queued item spawn: {}", spawnPacket.item.getDefinition().getName());
            renderer.onItemSpawn(spawnPacket);
        }

        ItemDespawnPacket despawnPacket;
        while ((despawnPacket = pendingItemDespawns.poll()) != null) {
            renderer.onItemDespawn(despawnPacket);
        }

        if (onGameWorldRendererReady != null) {
            onGameWorldRendererReady.run();
        }
    }

    public void setOnGameWorldRendererReady(Runnable callback) {
        this.onGameWorldRendererReady = callback;
    }

    public void setNetworkClient(NetworkClient networkClient) {
        if (this.networkClient != null) {
            this.networkClient.disconnect();
        }
        this.networkClient = networkClient;
        setupGlobalItemCallbacks(); // Reconfigurar callbacks após trocar cliente
    }

    @Override
    public void render() {
        super.render();
    }

    @Override
    public void dispose() {
        if (networkClient != null) {
            networkClient.disconnect();
        }
        super.dispose();
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
}