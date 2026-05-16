package com.sandbox.server;

import com.common.sandbox.model.player.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameWorld {
    private static final Logger logger = LoggerFactory.getLogger(GameWorld.class);
    private static GameWorld instance;

    private final ConcurrentHashMap<String, Player> onlinePlayers;
    private final ConcurrentHashMap<String, String> channelToPlayer;
    private final List<ChatMessage> chatHistory;

    private final ScheduledExecutorService scheduler;
    private static final int SAVE_INTERVAL_SECONDS = 30;
    private final Map<String, Long> lastSaveTime;

    private GameWorld() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.channelToPlayer = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
        this.lastSaveTime = new ConcurrentHashMap<>();

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startPeriodicSaveTask();
    }

    public static synchronized GameWorld getInstance() {
        if (instance == null) {
            instance = new GameWorld();
        }
        return instance;
    }

    private void startPeriodicSaveTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveAllPlayers();
            } catch (Exception e) {
                logger.error("❌ Erro no salvamento periódico", e);
            }
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);
        logger.info("🔄 Salvamento periódico iniciado (intervalo: {}s)", SAVE_INTERVAL_SECONDS);
    }

    private void saveAllPlayers() {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (Player player : onlinePlayers.values()) {
            Long lastSave = lastSaveTime.get(player.getId());
            if (lastSave == null || (now - lastSave) >= 5000) {
                DatabaseManager.getInstance().savePlayerAsync(player);
                lastSaveTime.put(player.getId(), now);
                saved++;
            }
        }

        if (saved > 0) {
            logger.debug("💾 Salvamento periódico: {} jogadores salvos", saved);
        }
    }

    public void addPlayer(Player player, String channelId) {
        onlinePlayers.put(player.getId(), player);
        channelToPlayer.put(channelId, player.getId());
        lastSaveTime.put(player.getId(), System.currentTimeMillis());

        DatabaseManager.getInstance().savePlayerAsync(player);

        logger.info("✅ Jogador entrou: {} | HP={}/{} | Pos=({},{})",
                player.getUsername(), player.getCurrentHp(), player.getMaxHp(),
                player.getX(), player.getY());
    }

    public void removePlayer(String channelId) {
        String playerId = channelToPlayer.remove(channelId);
        if (playerId != null) {
            Player player = onlinePlayers.remove(playerId);
            if (player != null) {
                player.setOnline(false);

                DatabaseManager.getInstance().savePlayer(player);
                DatabaseManager.getInstance().setPlayerOffline(playerId);
                lastSaveTime.remove(playerId);
                logger.info("❌ Jogador {} saiu. Final: HP={}/{}",
                        player.getUsername(), player.getCurrentHp(), player.getMaxHp());
            }
        }
    }

    public void updatePlayerPosition(String playerId, float x, float y, String direction) {
        Player player = onlinePlayers.get(playerId);
        if (player != null) {
            if (ChunkManager.getInstance().isSolid(x, y)) {
                return;
            }
            player.setX(x);
            player.setY(y);
            player.setDirection(direction);
            checkChunkTransition(player);
        }
    }


    public void savePlayer(Player player) {
        if (player == null) return;
        DatabaseManager.getInstance().savePlayerAsync(player);
        lastSaveTime.put(player.getId(), System.currentTimeMillis());
    }

    public void saveAllPlayersOnShutdown() {
        logger.info(" Salvando todos os {} jogadores antes do shutdown...", onlinePlayers.size());
        for (Player player : onlinePlayers.values()) {
            DatabaseManager.getInstance().savePlayer(player);
        }
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void checkChunkTransition(Player player) {
        int chunkSize = 32;
        int tileSize = 64;
        int currentChunkX = (int) Math.floor(player.getX() / (chunkSize * tileSize));
        int currentChunkY = (int) Math.floor(player.getY() / (chunkSize * tileSize));

        Integer lastChunkX = player.getLastChunkX();
        Integer lastChunkY = player.getLastChunkY();

        if (lastChunkX == null || lastChunkX != currentChunkX || lastChunkY != currentChunkY) {
            player.setLastChunkX(currentChunkX);
            player.setLastChunkY(currentChunkY);
        }
    }

    public void addChatMessage(String playerId, String playerName, String message) {
        ChatMessage msg = new ChatMessage(playerId, playerName, message);
        chatHistory.add(msg);
        while (chatHistory.size() > 100) {
            chatHistory.remove(0);
        }
        DatabaseManager.getInstance().saveChatMessage(playerId, message);
    }

    public Player getPlayer(String id) {
        return onlinePlayers.get(id);
    }

    public String getPlayerIdByChannel(String channelId) {
        return channelToPlayer.get(channelId);
    }

    public Collection<Player> getAllPlayers() {
        return onlinePlayers.values();
    }

    public void cleanOldChatMessages() {
        long cutoff = System.currentTimeMillis() - 300000;
        chatHistory.removeIf(msg -> msg.timestamp < cutoff);
    }

    public void checkInactivePlayers() {}

    public static class ChatMessage {
        public String playerId;
        public String playerName;
        public String message;
        public long timestamp;
        public ChatMessage(String playerId, String playerName, String message) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
    }
}