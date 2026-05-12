package com.sandbox.server;

import com.common.sandbox.model.Player;
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

    private final ConcurrentHashMap<String, Player> onlinePlayers;  // playerId -> Player
    private final ConcurrentHashMap<String, String> channelToPlayer; // channelId -> playerId
    private final List<ChatMessage> chatHistory;

    // Para salvamento periódico
    private final ScheduledExecutorService scheduler;
    private static final int SAVE_INTERVAL_SECONDS = 60; // Salvar a cada 60 segundos
    private final Map<String, Long> lastPositionSave; // playerId -> lastSaveTime

    private GameWorld() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.channelToPlayer = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
        this.lastPositionSave = new ConcurrentHashMap<>();

        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startPeriodicSaveTask();
    }

    public static synchronized GameWorld getInstance() {
        if (instance == null) {
            instance = new GameWorld();
        }
        return instance;
    }

    /**
     * Inicia tarefa periódica para salvar posições de todos os players
     */
    private void startPeriodicSaveTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                saveAllPlayerPositions();
            } catch (Exception e) {
                logger.error("❌ Erro no salvamento periódico de posições", e);
            }
        }, SAVE_INTERVAL_SECONDS, SAVE_INTERVAL_SECONDS, TimeUnit.SECONDS);

        logger.info("🔄 Salvamento periódico de posições iniciado (intervalo: {}s)", SAVE_INTERVAL_SECONDS);
    }

    /**
     * Salva posição de todos os players online
     */
    private void saveAllPlayerPositions() {
        long now = System.currentTimeMillis();
        int saved = 0;

        for (Player player : onlinePlayers.values()) {
            Long lastSave = lastPositionSave.get(player.getId());

            // Salvar se nunca salvou ou se passou do intervalo
            if (lastSave == null || (now - lastSave) >= (SAVE_INTERVAL_SECONDS * 1000)) {
                DatabaseManager.getInstance().savePlayerPositionAsync(player);
                lastPositionSave.put(player.getId(), now);
                saved++;
            }
        }

        if (saved > 0) {
            logger.debug("💾 Salvamento periódico: {} posições atualizadas", saved);
        }
    }

    public void addPlayer(Player player, String channelId) {
        onlinePlayers.put(player.getId(), player);
        channelToPlayer.put(channelId, player.getId());
        lastPositionSave.put(player.getId(), System.currentTimeMillis());
        logger.info("✅ Jogador entrou: {} na posição ({}, {}). Total: {}",
                player.getUsername(), player.getX(), player.getY(), onlinePlayers.size());
    }

    public void removePlayer(String channelId) {
        String playerId = channelToPlayer.remove(channelId);
        if (playerId != null) {
            Player player = onlinePlayers.remove(playerId);
            if (player != null) {
                // Salvar posição final
                DatabaseManager.getInstance().savePlayerPosition(player);
                lastPositionSave.remove(playerId);
                logger.info("❌ Jogador {} removido do mundo. Posição final ({}, {})",
                        player.getUsername(), player.getX(), player.getY());
            }
        }
    }

    public void updatePlayerPosition(String playerId, float x, float y, String direction) {
        Player player = onlinePlayers.get(playerId);
        if (player != null) {
            // Verificar colisão
            if (ChunkManager.getInstance().isSolid(x, y)) {
                logger.warn("⚠️ Tentativa de mover jogador {} para posição sólida ({}, {})",
                        player.getUsername(), x, y);
                return;
            }

            player.setX(x);
            player.setY(y);
            player.setDirection(direction);

            // FORÇAR SALVAMENTO PERIÓDICO MAIS FREQUENTE
            long now = System.currentTimeMillis();
            Long lastSave = lastPositionSave.get(playerId);
            if (lastSave == null || (now - lastSave) >= 5000) { // Salvar a cada 5 segundos
                DatabaseManager.getInstance().savePlayerPositionAsync(player);
                lastPositionSave.put(playerId, now);
            }

            // Verificar mudança de chunk
            checkChunkTransition(player);
        }
    }

    // Método para forçar salvamento imediato (usado em eventos importantes)
    public void forceSavePlayerPosition(String playerId) {
        Player player = onlinePlayers.get(playerId);
        if (player != null) {
            DatabaseManager.getInstance().savePlayerPositionAsync(player);
            lastPositionSave.put(playerId, System.currentTimeMillis());
            logger.debug("💾 Forçado salvamento de posição para {}", player.getUsername());
        }
    }

    // Salvar todos os players (usado no shutdown do server)
    public void saveAllPlayersOnShutdown() {
        logger.info("💾 Salvando posições de todos os {} jogadores antes do shutdown...", onlinePlayers.size());
        for (Player player : onlinePlayers.values()) {
            DatabaseManager.getInstance().savePlayerPosition(player);
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
            logger.debug("🔄 Player {} moveu para chunk [{},{}]",
                    player.getUsername(), currentChunkX, currentChunkY);
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

    public void checkInactivePlayers() {
        long timeout = 60000; // 60 segundos sem movimento?
        // Implementar se necessário
    }

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