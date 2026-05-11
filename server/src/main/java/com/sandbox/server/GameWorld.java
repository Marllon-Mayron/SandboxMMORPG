package com.sandbox.server;

import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameWorld {
    private static final Logger logger = LoggerFactory.getLogger(GameWorld.class);
    private static GameWorld instance;

    private final ConcurrentHashMap<String, Player> onlinePlayers;  // String ID
    private final ConcurrentHashMap<String, String> channelToPlayer;
    private final List<ChatMessage> chatHistory;

    private GameWorld() {
        this.onlinePlayers = new ConcurrentHashMap<>();
        this.channelToPlayer = new ConcurrentHashMap<>();
        this.chatHistory = new ArrayList<>();
    }

    public String getPlayerIdByChannel(String channelId) {
        return channelToPlayer.get(channelId);
    }
    public static synchronized GameWorld getInstance() {
        if (instance == null) {
            instance = new GameWorld();
        }
        return instance;
    }

    public void addPlayer(Player player, String channelId) {
        onlinePlayers.put(player.getId(), player);
        channelToPlayer.put(channelId, player.getId());
        logger.info("✅ Jogador entrou: {}. Total: {}", player.getUsername(), onlinePlayers.size());
    }

    public void removePlayer(String channelId) {
        String playerId = channelToPlayer.remove(channelId);
        if (playerId != null) {
            Player player = onlinePlayers.remove(playerId);
            if (player != null) {
                logger.info("❌ Jogador saiu: {}. Total: {}", player.getUsername(), onlinePlayers.size());
            }
        }
    }

    public Player getPlayer(String id) {
        return onlinePlayers.get(id);
    }

    public Collection<Player> getAllPlayers() {
        return onlinePlayers.values();
    }

    public void updatePlayerPosition(String playerId, float x, float y, String direction) {
        Player player = onlinePlayers.get(playerId);
        if (player != null) {
            // VERIFICAR SE A POSIÇÃO É VÁLIDA (COLISÃO)
            if (ChunkManager.getInstance().isSolid(x, y)) {
                logger.warn("⚠️ Tentativa de mover jogador {} para posição sólida ({}, {})",
                        player.getUsername(), x, y);
                return;
            }

            player.setX(x);
            player.setY(y);
            player.setDirection(direction);

            // VERIFICAR MUDANÇA DE CHUNK
            checkChunkTransition(player);
        }
    }

    // NOVO: Detectar quando jogador muda de chunk para carregar novas áreas
    private void checkChunkTransition(Player player) {
        int currentChunkX = (int) Math.floor(player.getX() / (32 * 32));  // 32 tiles * 32px
        int currentChunkY = (int) Math.floor(player.getY() / (32 * 32));

        // Armazenar último chunk no Player (adicione este campo à classe Player)
        Integer lastChunkX = player.getLastChunkX();
        Integer lastChunkY = player.getLastChunkY();

        if (lastChunkX == null || lastChunkX != currentChunkX || lastChunkY != currentChunkY) {
            player.setLastChunkX(currentChunkX);
            player.setLastChunkY(currentChunkY);
            logger.debug("🔄 Player {} moveu para chunk [{},{}]",
                    player.getUsername(), currentChunkX, currentChunkY);

            // TODO: Enviar novas chunks para o cliente
            // Isso será implementado com o sistema de chunks
        }
    }

    public void addChatMessage(String playerId, String playerName, String message) {
        ChatMessage msg = new ChatMessage(playerId, playerName, message);
        chatHistory.add(msg);

        // Manter apenas últimas 100 mensagens
        while (chatHistory.size() > 100) {
            chatHistory.remove(0);
        }

        DatabaseManager.getInstance().saveChatMessage(playerId, message);
    }

    public void cleanOldChatMessages() {
        long cutoff = System.currentTimeMillis() - 300000;
        chatHistory.removeIf(msg -> msg.timestamp < cutoff);
    }

    public void checkInactivePlayers() {
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