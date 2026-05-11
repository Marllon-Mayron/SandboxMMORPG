package com.sandbox.server;

import com.common.sandbox.model.MapJSON;
import com.common.sandbox.model.Player;
import com.common.sandbox.network.packets.*;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.timeout.ReadTimeoutException;
import io.netty.util.concurrent.GlobalEventExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GameServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final Logger logger = LoggerFactory.getLogger(GameServerHandler.class);
    private String channelId;
    private Player currentPlayer;
    private static final ChannelGroup channels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    public GameServerHandler() {}

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        this.channelId = ctx.channel().id().asLongText();
        channels.add(ctx.channel());
        logger.info("🔌 Nova conexão: {} | Total conectados: {}", channelId, channels.size());
    }

    public static ChannelGroup getChannels() {
        return channels;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            logger.info("📦 Pacote recebido - Tipo: {}", msg.getClass().getSimpleName());

            if (msg instanceof HandshakePacket) {
                HandshakePacket handshake = (HandshakePacket) msg;
                logger.info("🤝 HANDSHAKE recebido - Versão: {}, Cliente: {}", handshake.version, handshake.clientId);

            } else if (msg instanceof LoginRequest) {
                handleLogin(ctx, (LoginRequest) msg);

            } else if (msg instanceof RegisterRequest) {
                handleRegister(ctx, (RegisterRequest) msg);

            } else if (msg instanceof MovementRequest) {
                if (currentPlayer != null) {
                    handleMovement(ctx, (MovementRequest) msg);
                }

            } else if (msg instanceof ChatMessage) {
                if (currentPlayer != null) {
                    handleChat(ctx, (ChatMessage) msg);
                }

            } else if (msg instanceof MapSaveRequest) {
                logger.info("🗺️ MAP_SAVE_REQUEST recebido");
                handleMapSave(ctx, (MapSaveRequest) msg);

            } else if (msg instanceof MapLoadRequest) {
                logger.info("🗺️ MAP_LOAD_REQUEST recebido");
                handleMapLoad(ctx, (MapLoadRequest) msg);

            } else if (msg instanceof PingPacket) {
                logger.debug("Ping recebido");
            } else {
                logger.warn("⚠️ Tipo de pacote desconhecido: {}", msg.getClass().getSimpleName());
            }

        } catch (Exception e) {
            logger.error("❌ Erro ao processar pacote: {}", e.getMessage(), e);
        }
    }

    private void handleLogin(ChannelHandlerContext ctx, LoginRequest request) {
        try {
            logger.info("📥 LOGIN - Usuário: {}", request.username);

            Player player = DatabaseManager.getInstance().authenticatePlayer(
                    request.username,
                    request.password
            );

            if (player != null) {
                logger.info("✅ LOGIN SUCESSO - Usuário: {}", request.username);
                currentPlayer = player;
                GameWorld.getInstance().addPlayer(player, channelId);

                LoginResponse response = new LoginResponse(true, "Login bem-sucedido!", player);
                response.nearbyPlayers = new java.util.HashMap<>();

                for (Player p : GameWorld.getInstance().getAllPlayers()) {
                    if (!p.getId().equals(player.getId())) {
                        response.nearbyPlayers.put(p.getId(), p);
                    }
                }

                sendPacket(ctx, response);
                logger.info("📤 RESPOSTA LOGIN enviada com {} jogadores próximos", response.nearbyPlayers.size());

                ChatMessage joinMsg = new ChatMessage(player.getId(), "SISTEMA",
                        "✨ " + player.getUsername() + " entrou no mundo!");
                broadcastToAll(joinMsg);
                broadcastToAll(new MovementBroadcast(player));

                // Enviar o mapa para o jogador
                sendMapToPlayer(ctx, player);

            } else {
                logger.warn("❌ LOGIN FALHOU - Usuário: {}", request.username);
                LoginResponse response = new LoginResponse(false, "Usuário ou senha inválidos!", null);
                sendPacket(ctx, response);
            }
        } catch (Exception e) {
            logger.error("❌ ERRO handleLogin: {}", e.getMessage(), e);
        }
    }

    private void sendMapToPlayer(ChannelHandlerContext ctx, Player player) {
        try {
            MapJSON map = ChunkManager.getInstance().getMap();
            if (map != null) {
                MapLoadResponse response = new MapLoadResponse(true, "Mapa carregado!", map);
                sendPacket(ctx, response);
                logger.info("📦 Mapa enviado para o jogador: {} chunks", map.getChunks().size());
            }
        } catch (Exception e) {
            logger.error("Erro ao enviar mapa para o jogador", e);
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, RegisterRequest request) {
        try {
            logger.info("📥 REGISTRO - Usuário: {}, Email: {}", request.username, request.email);

            boolean success = DatabaseManager.getInstance().registerPlayer(
                    request.username,
                    request.email,
                    request.password
            );

            RegisterResponse response = new RegisterResponse(
                    success,
                    success ? "✅ Registro bem-sucedido!" : "❌ Usuário ou email já existe!"
            );

            sendPacket(ctx, response);
        } catch (Exception e) {
            logger.error("❌ ERRO handleRegister: {}", e.getMessage(), e);
        }
    }

    private void handleMovement(ChannelHandlerContext ctx, MovementRequest request) {
        try {
            if (!canMoveTo(request.x, request.y)) {
                Player currentPlayerState = GameWorld.getInstance().getPlayer(request.playerId);
                if (currentPlayerState != null) {
                    MovementBroadcast correction = new MovementBroadcast(currentPlayerState);
                    sendPacket(ctx, correction);
                }
                return;
            }

            GameWorld.getInstance().updatePlayerPosition(request.playerId, request.x, request.y, request.direction);

            Player updatedPlayer = GameWorld.getInstance().getPlayer(request.playerId);
            if (updatedPlayer != null) {
                broadcastToAll(new MovementBroadcast(updatedPlayer));
            }
        } catch (Exception e) {
            logger.error("❌ ERRO handleMovement: {}", e.getMessage(), e);
        }
    }

    private boolean canMoveTo(float x, float y) {
        return !ChunkManager.getInstance().isSolid(x, y);
    }

    private void handleChat(ChannelHandlerContext ctx, ChatMessage chatMsg) {
        try {
            if (chatMsg.message == null || chatMsg.message.trim().isEmpty()) return;

            if (chatMsg.message.length() > 500) {
                chatMsg.message = chatMsg.message.substring(0, 500);
            }

            GameWorld.getInstance().addChatMessage(currentPlayer.getId(), currentPlayer.getUsername(), chatMsg.message);
            broadcastToAll(chatMsg);
            logger.info("💬 {}: {}", currentPlayer.getUsername(), chatMsg.message);
        } catch (Exception e) {
            logger.error("❌ ERRO handleChat: {}", e.getMessage());
        }
    }

    private void handleMapSave(ChannelHandlerContext ctx, MapSaveRequest request) {
        try {
            MapJSON map = request.mapData;
            logger.info("💾 Salvando mapa: {} ({} chunks)", map.getMapName(), map.getChunks().size());

            ChunkManager.getInstance().saveMap(map);

            MapSaveResponse response = new MapSaveResponse(true, "Mapa salvo com sucesso!", map.getMapId());
            sendPacket(ctx, response);
            logger.info("✅ Mapa salvo com sucesso: {}", map.getMapId());

        } catch (Exception e) {
            logger.error("❌ Erro ao salvar mapa: {}", e.getMessage(), e);
            MapSaveResponse response = new MapSaveResponse(false, "Erro ao salvar: " + e.getMessage(), null);
            sendPacket(ctx, response);
        }
    }

    private void handleMapLoad(ChannelHandlerContext ctx, MapLoadRequest request) {
        try {
            String mapId = request.mapId;
            logger.info("📥 Carregando mapa: {}", mapId);

            MapJSON map = ChunkManager.getInstance().getMap(mapId);

            if (map != null && !map.getChunks().isEmpty()) {
                MapLoadResponse response = new MapLoadResponse(true, "Mapa carregado com sucesso!", map);
                sendPacket(ctx, response);
                logger.info("✅ Mapa carregado: {} chunks", map.getChunks().size());
            } else {
                // Criar mapa vazio se não existir
                MapJSON emptyMap = new MapJSON(mapId, "sandbox_world");
                MapLoadResponse response = new MapLoadResponse(true, "Mapa vazio criado!", emptyMap);
                sendPacket(ctx, response);
                logger.info("📦 Mapa vazio criado para: {}", mapId);
            }

        } catch (Exception e) {
            logger.error("❌ Erro ao carregar mapa: {}", e.getMessage(), e);
            MapLoadResponse response = new MapLoadResponse(false, "Erro ao carregar: " + e.getMessage(), null);
            sendPacket(ctx, response);
        }
    }

    private void sendPacket(ChannelHandlerContext ctx, Object packet) {
        ctx.writeAndFlush(packet);
    }

    public static void broadcastToAll(Object packet) {
        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(packet);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            logger.info("⏰ Timeout: {}", channelId);
        } else {
            logger.error("❌ Erro: {}", cause.getMessage());
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (currentPlayer != null) {
            GameWorld.getInstance().removePlayer(channelId);
            ChatMessage leaveMsg = new ChatMessage(currentPlayer.getId(), "SISTEMA",
                    "👋 " + currentPlayer.getUsername() + " saiu!");
            broadcastToAll(leaveMsg);
            logger.info("📤 {} desconectado", currentPlayer.getUsername());
        }
        channels.remove(ctx.channel());
        logger.info("🔌 Conexão fechada: {}", channelId);
    }
}