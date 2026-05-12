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

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
        logger.info("Nova conexao: {} | Total conectados: {}", channelId, channels.size());
    }

    public static ChannelGroup getChannels() {
        return channels;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        try {
            logger.debug("Pacote recebido - Tipo: {}", msg.getClass().getSimpleName());

            if (msg instanceof HandshakePacket) {
                HandshakePacket handshake = (HandshakePacket) msg;
                logger.info("Handshake recebido - Versao: {}, Cliente: {}", handshake.version, handshake.clientId);

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
                logger.info("MapSaveRequest recebido");
                handleMapSave(ctx, (MapSaveRequest) msg);

            } else if (msg instanceof MapLoadRequest) {
                logger.info("MapLoadRequest recebido");
                handleMapLoad(ctx, (MapLoadRequest) msg);

            } else if (msg instanceof PingPacket) {
                logger.debug("Ping recebido");

            } else if (msg instanceof FriendRequestPacket) {
                if (currentPlayer != null) {
                    handleFriendRequest(ctx, (FriendRequestPacket) msg);
                }

            } else if (msg instanceof PrivateMessagePacket) {
                if (currentPlayer != null) {
                    handlePrivateMessage(ctx, (PrivateMessagePacket) msg);
                }

            } else if (msg instanceof PrivateMessageHistoryRequest) {
                handlePrivateMessageHistory(ctx, (PrivateMessageHistoryRequest) msg);
            } else {
                logger.warn("Tipo de pacote desconhecido: {}", msg.getClass().getSimpleName());
            }

        } catch (Exception e) {
            logger.error("Erro ao processar pacote: {}", e.getMessage(), e);
        }
    }

    // ==================== LOGIN E REGISTRO ====================

    private void handleLogin(ChannelHandlerContext ctx, LoginRequest request) {
        try {
            logger.info("Login - Usuario: {}", request.username);

            Player player = DatabaseManager.getInstance().authenticatePlayer(
                    request.username,
                    request.password
            );

            if (player != null) {
                logger.info("Login Sucesso - Usuario: {}", request.username);
                currentPlayer = player;
                GameWorld.getInstance().addPlayer(player, channelId);

                LoginResponse response = new LoginResponse(true, "Login bem-sucedido!", player);
                response.nearbyPlayers = new java.util.HashMap<>();

                for (Player p : GameWorld.getInstance().getAllPlayers()) {
                    if (!p.getId().equals(player.getId())) {
                        response.nearbyPlayers.put(p.getId(), p);
                        logger.debug("Adicionando jogador existente: {} na posicao ({}, {})",
                                p.getUsername(), p.getX(), p.getY());
                    }
                }

                sendPacket(ctx, response);
                logger.info("Resposta login enviada com {} jogadores ja conectados",
                        response.nearbyPlayers.size());

                ChatMessage joinMsg = new ChatMessage(player.getId(), "SISTEMA",
                        player.getUsername() + " entrou no mundo!");
                broadcastToAll(joinMsg);

                broadcastToAll(new MovementBroadcast(player));
                sendMapToPlayer(ctx, player);

                // Enviar lista de amigos para o jogador logado
                sendFriendListToPlayer(ctx, player);

            } else {
                logger.warn("Login Falhou - Usuario: {}", request.username);
                LoginResponse response = new LoginResponse(false, "Usuario ou senha invalidos!", null);
                sendPacket(ctx, response);
            }
        } catch (Exception e) {
            logger.error("Erro handleLogin: {}", e.getMessage(), e);
        }
    }

    private void sendMapToPlayer(ChannelHandlerContext ctx, Player player) {
        try {
            MapJSON map = ChunkManager.getInstance().getMap();
            if (map != null) {
                MapLoadResponse response = new MapLoadResponse(true, "Mapa carregado!", map);
                sendPacket(ctx, response);
                logger.info("Mapa enviado para o jogador: {} chunks", map.getChunks().size());
            }
        } catch (Exception e) {
            logger.error("Erro ao enviar mapa para o jogador", e);
        }
    }

    private void handleRegister(ChannelHandlerContext ctx, RegisterRequest request) {
        try {
            logger.info("Registro - Usuario: {}, Email: {}", request.username, request.email);

            boolean success = DatabaseManager.getInstance().registerPlayer(
                    request.username,
                    request.email,
                    request.password
            );

            RegisterResponse response = new RegisterResponse(
                    success,
                    success ? "Registro bem-sucedido!" : "Usuario ou email ja existe!"
            );

            sendPacket(ctx, response);
        } catch (Exception e) {
            logger.error("Erro handleRegister: {}", e.getMessage(), e);
        }
    }

    // ==================== MOVIMENTO ====================

    private void handleMovement(ChannelHandlerContext ctx, MovementRequest request) {
        try {
            Player player = GameWorld.getInstance().getPlayer(request.playerId);
            if (player == null) {
                logger.warn("Movimento de jogador inexistente: {}", request.playerId);
                return;
            }

            if (!canMoveTo(request.x, request.y)) {
                MovementBroadcast correction = new MovementBroadcast(player);
                sendPacket(ctx, correction);
                return;
            }

            GameWorld.getInstance().updatePlayerPosition(request.playerId, request.x, request.y, request.direction);

            Player updatedPlayer = GameWorld.getInstance().getPlayer(request.playerId);
            if (updatedPlayer != null) {
                broadcastToAllExcept(new MovementBroadcast(updatedPlayer), ctx.channel().id().asLongText());
            }
        } catch (Exception e) {
            logger.error("Erro handleMovement: {}", e.getMessage(), e);
        }
    }

    private boolean canMoveTo(float x, float y) {
        return !ChunkManager.getInstance().isSolid(x, y);
    }

    // ==================== CHAT ====================

    private void handleChat(ChannelHandlerContext ctx, ChatMessage chatMsg) {
        try {
            if (chatMsg.message == null || chatMsg.message.trim().isEmpty()) return;

            if (chatMsg.message.length() > 500) {
                chatMsg.message = chatMsg.message.substring(0, 500);
            }

            GameWorld.getInstance().addChatMessage(currentPlayer.getId(), currentPlayer.getUsername(), chatMsg.message);
            broadcastToAll(chatMsg);
            logger.info("{}: {}", currentPlayer.getUsername(), chatMsg.message);
        } catch (Exception e) {
            logger.error("Erro handleChat: {}", e.getMessage());
        }
    }

    // ==================== MAPA ====================

    private void handleMapSave(ChannelHandlerContext ctx, MapSaveRequest request) {
        try {
            MapJSON map = request.mapData;
            logger.info("Salvando mapa: {} ({} chunks)", map.getMapName(), map.getChunks().size());

            ChunkManager.getInstance().saveMap(map);

            MapSaveResponse response = new MapSaveResponse(true, "Mapa salvo com sucesso!", map.getMapId());
            sendPacket(ctx, response);
            logger.info("Mapa salvo com sucesso: {}", map.getMapId());

        } catch (Exception e) {
            logger.error("Erro ao salvar mapa: {}", e.getMessage(), e);
            MapSaveResponse response = new MapSaveResponse(false, "Erro ao salvar: " + e.getMessage(), null);
            sendPacket(ctx, response);
        }
    }

    private void handleMapLoad(ChannelHandlerContext ctx, MapLoadRequest request) {
        try {
            String mapId = request.mapId;
            logger.info("Carregando mapa: {}", mapId);

            MapJSON map = ChunkManager.getInstance().getMap(mapId);

            if (map != null && !map.getChunks().isEmpty()) {
                MapLoadResponse response = new MapLoadResponse(true, "Mapa carregado com sucesso!", map);
                sendPacket(ctx, response);
                logger.info("Mapa carregado: {} chunks", map.getChunks().size());
            } else {
                MapJSON emptyMap = new MapJSON(mapId, "sandbox_world");
                MapLoadResponse response = new MapLoadResponse(true, "Mapa vazio criado!", emptyMap);
                sendPacket(ctx, response);
                logger.info("Mapa vazio criado para: {}", mapId);
            }

        } catch (Exception e) {
            logger.error("Erro ao carregar mapa: {}", e.getMessage(), e);
            MapLoadResponse response = new MapLoadResponse(false, "Erro ao carregar: " + e.getMessage(), null);
            sendPacket(ctx, response);
        }
    }

    // ==================== SISTEMA DE AMIGOS ====================

    private void handleFriendRequest(ChannelHandlerContext ctx, FriendRequestPacket packet) {
        try {
            if (currentPlayer == null) return;

            String action = packet.action;
            logger.info("Friend request - Action: {}, Target: {}", action, packet.targetUsername);

            switch (action) {
                case "SEND":
                    sendFriendRequest(ctx, packet.targetUsername);
                    break;
                case "ACCEPT":
                    acceptFriendRequest(ctx, packet.requestId);
                    break;
                case "REJECT":
                    rejectFriendRequest(ctx, packet.requestId);
                    break;
                case "REMOVE":
                    removeFriend(ctx, packet.targetUsername);
                    break;
                case "LIST":
                    sendFriendListToPlayer(ctx, currentPlayer);
                    break;
                default:
                    logger.warn("Unknown friend action: {}", action);
            }
        } catch (Exception e) {
            logger.error("Erro handleFriendRequest: {}", e.getMessage(), e);
        }
    }

    private void sendFriendRequest(ChannelHandlerContext ctx, String targetUsername) {
        try {
            // Buscar o jogador alvo pelo username
            Player targetPlayer = DatabaseManager.getInstance().getPlayerByUsername(targetUsername);

            if (targetPlayer == null) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Jogador nao encontrado!";
                sendPacket(ctx, response);
                return;
            }

            if (targetPlayer.getId().equals(currentPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Voce nao pode adicionar a si mesmo!";
                sendPacket(ctx, response);
                return;
            }

            // Verificar se ja sao amigos
            if (DatabaseManager.getInstance().areFriends(currentPlayer.getId(), targetPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Voce ja e amigo deste jogador!";
                sendPacket(ctx, response);
                return;
            }

            // Verificar se ja existe solicitacao pendente
            if (DatabaseManager.getInstance().hasPendingRequest(currentPlayer.getId(), targetPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Solicitacao de amizade ja enviada!";
                sendPacket(ctx, response);
                return;
            }

            // Criar solicitacao
            String requestId = DatabaseManager.getInstance().createFriendRequest(currentPlayer.getId(), targetPlayer.getId());

            if (requestId != null) {
                // Notificar o destinatario se estiver online
                Channel targetChannel = getChannelByPlayerId(targetPlayer.getId());
                if (targetChannel != null) {
                    FriendRequestPacket notification = new FriendRequestPacket("NEW_REQUEST", currentPlayer.getUsername());
                    notification.requestId = requestId;
                    notification.fromPlayerId = currentPlayer.getId();
                    notification.fromUsername = currentPlayer.getUsername();
                    notification.fromLevel = currentPlayer.getLevel();
                    targetChannel.writeAndFlush(notification);
                }

                FriendRequestPacket response = new FriendRequestPacket("SENT", targetUsername);
                response.success = true;
                response.message = "Solicitacao de amizade enviada!";
                sendPacket(ctx, response);
            }

        } catch (SQLException e) {
            logger.error("Erro sendFriendRequest: {}", e.getMessage(), e);
            FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
            response.message = "Erro ao enviar solicitacao!";
            sendPacket(ctx, response);
        }
    }

    private void acceptFriendRequest(ChannelHandlerContext ctx, String requestId) {
        try {
            // Buscar detalhes da solicitacao - use DatabaseManager.FriendRequestDetails
            DatabaseManager.FriendRequestDetails details = DatabaseManager.getInstance().getFriendRequestDetails(requestId);

            if (details == null || !details.toPlayerId.equals(currentPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", "");
                response.message = "Solicitacao invalida!";
                sendPacket(ctx, response);
                return;
            }

            // Aceitar amizade
            boolean success = DatabaseManager.getInstance().acceptFriendRequest(requestId);

            if (success) {
                // Notificar o solicitante se estiver online
                Channel requesterChannel = getChannelByPlayerId(details.fromPlayerId);
                if (requesterChannel != null) {
                    FriendRequestPacket notification = new FriendRequestPacket("ACCEPTED", currentPlayer.getUsername());
                    notification.success = true;
                    notification.fromPlayerId = currentPlayer.getId();
                    notification.fromUsername = currentPlayer.getUsername();
                    notification.fromLevel = currentPlayer.getLevel();
                    requesterChannel.writeAndFlush(notification);
                }

                FriendRequestPacket response = new FriendRequestPacket("ACCEPTED", details.fromUsername);
                response.success = true;
                response.message = "Solicitacao aceita!";
                sendPacket(ctx, response);

                // Enviar lista de amigos atualizada para ambos
                sendFriendListToPlayer(ctx, currentPlayer);
                if (requesterChannel != null) {
                    Player requester = GameWorld.getInstance().getPlayer(details.fromPlayerId);
                    if (requester != null) {
                        sendFriendListToPlayer(requesterChannel, requester);
                    }
                }
            }

        } catch (SQLException e) {
            logger.error("Erro acceptFriendRequest: {}", e.getMessage(), e);
            FriendRequestPacket response = new FriendRequestPacket("ERROR", "");
            response.message = "Erro ao aceitar solicitacao!";
            sendPacket(ctx, response);
        }
    }

    private void rejectFriendRequest(ChannelHandlerContext ctx, String requestId) {
        try {
            boolean success = DatabaseManager.getInstance().rejectFriendRequest(requestId);

            if (success) {
                FriendRequestPacket response = new FriendRequestPacket("REJECTED", "");
                response.success = true;
                response.message = "Solicitacao recusada!";
                sendPacket(ctx, response);

                // Enviar lista atualizada
                sendFriendListToPlayer(ctx, currentPlayer);
            }

        } catch (SQLException e) {
            logger.error("Erro rejectFriendRequest: {}", e.getMessage(), e);
            FriendRequestPacket response = new FriendRequestPacket("ERROR", "");
            response.message = "Erro ao recusar solicitacao!";
            sendPacket(ctx, response);
        }
    }

    private void removeFriend(ChannelHandlerContext ctx, String friendUsername) {
        try {
            Player friend = DatabaseManager.getInstance().getPlayerByUsername(friendUsername);

            if (friend == null) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", friendUsername);
                response.message = "Amigo nao encontrado!";
                sendPacket(ctx, response);
                return;
            }

            boolean success = DatabaseManager.getInstance().removeFriend(currentPlayer.getId(), friend.getId());

            if (success) {
                // Notificar o amigo se estiver online
                Channel friendChannel = getChannelByPlayerId(friend.getId());
                if (friendChannel != null) {
                    FriendRequestPacket notification = new FriendRequestPacket("REMOVED", currentPlayer.getUsername());
                    notification.success = true;
                    friendChannel.writeAndFlush(notification);
                    sendFriendListToPlayer(friendChannel, friend);
                }

                FriendRequestPacket response = new FriendRequestPacket("REMOVED", friendUsername);
                response.success = true;
                response.message = "Amigo removido!";
                sendPacket(ctx, response);

                // Enviar lista atualizada
                sendFriendListToPlayer(ctx, currentPlayer);
            }

        } catch (SQLException e) {
            logger.error("Erro removeFriend: {}", e.getMessage(), e);
            FriendRequestPacket response = new FriendRequestPacket("ERROR", friendUsername);
            response.message = "Erro ao remover amigo!";
            sendPacket(ctx, response);
        }
    }

    private void sendFriendListToPlayer(ChannelHandlerContext ctx, Player player) {
        try {
            FriendListResponse response = DatabaseManager.getInstance().getFriendList(player.getId());
            sendPacket(ctx, response);
            logger.info("Lista de amigos enviada para {}: {} amigos, {} solicitacoes",
                    player.getUsername(),
                    response.friends != null ? response.friends.size() : 0,
                    response.pendingRequests != null ? response.pendingRequests.size() : 0);
        } catch (SQLException e) {
            logger.error("Erro sendFriendListToPlayer: {}", e.getMessage(), e);
        }
    }

    private void sendFriendListToPlayer(Channel channel, Player player) {
        try {
            FriendListResponse response = DatabaseManager.getInstance().getFriendList(player.getId());
            channel.writeAndFlush(response);
        } catch (SQLException e) {
            logger.error("Erro sendFriendListToPlayer: {}", e.getMessage(), e);
        }
    }

    private Channel getChannelByPlayerId(String playerId) {
        for (Channel channel : channels) {
            GameServerHandler handler = (GameServerHandler) channel.pipeline().last();
            if (handler != null && handler.currentPlayer != null &&
                    handler.currentPlayer.getId().equals(playerId)) {
                return channel;
            }
        }
        return null;
    }

    // ==================== MENSAGENS PRIVADAS ====================

    private void handlePrivateMessage(ChannelHandlerContext ctx, PrivateMessagePacket packet) {
        try {
            if (currentPlayer == null) return;

            // Verificar se sao amigos
            if (!DatabaseManager.getInstance().areFriends(currentPlayer.getId(), packet.toPlayerId)) {
                PrivateMessagePacket error = new PrivateMessagePacket();
                error.message = "Voce so pode enviar mensagens para amigos!";
                sendPacket(ctx, error);
                return;
            }

            // Buscar informacoes do destinatario
            Player targetPlayer = DatabaseManager.getInstance().getPlayerById(packet.toPlayerId);
            if (targetPlayer == null) return;

            packet.fromPlayerId = currentPlayer.getId();
            packet.fromUsername = currentPlayer.getUsername();
            packet.timestamp = System.currentTimeMillis();

            // Salvar no banco
            DatabaseManager.getInstance().savePrivateMessage(packet);

            // Enviar para o destinatario se estiver online
            Channel targetChannel = getChannelByPlayerId(packet.toPlayerId);
            if (targetChannel != null) {
                targetChannel.writeAndFlush(packet);
            }

        } catch (SQLException e) {
            logger.error("Erro handlePrivateMessage: {}", e.getMessage(), e);
        }
    }

    // ==================== UTILITARIOS ====================

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

    public static void broadcastToAllExcept(Object packet, String excludeChannelId) {
        for (Channel channel : channels) {
            if (channel.isActive() && !channel.id().asLongText().equals(excludeChannelId)) {
                channel.writeAndFlush(packet);
            }
        }
    }

    private void handlePrivateMessageHistory(ChannelHandlerContext ctx, PrivateMessageHistoryRequest request) {
        try {
            if (currentPlayer == null) return;

            List<PrivateMessagePacket> messages = DatabaseManager.getInstance()
                    .getPrivateMessageHistory(currentPlayer.getId(), request.friendId, request.limit);

            PrivateMessageHistoryResponse response = new PrivateMessageHistoryResponse(request.friendId, messages);
            sendPacket(ctx, response);

            // Marcar como lidas
            DatabaseManager.getInstance().markMessagesAsRead(currentPlayer.getId(), request.friendId);

        } catch (SQLException e) {
            logger.error("Erro handlePrivateMessageHistory: {}", e.getMessage(), e);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (cause instanceof ReadTimeoutException) {
            logger.info("Timeout: {}", channelId);
        } else {
            logger.error("Erro: {}", cause.getMessage());
        }
        ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        if (currentPlayer != null) {
            PlayerLeftPacket leftPacket = new PlayerLeftPacket(currentPlayer.getId(), currentPlayer.getUsername());
            GameServerHandler.broadcastToAll(leftPacket);

            ChatMessage leaveMsg = new ChatMessage(currentPlayer.getId(), "SISTEMA",
                    currentPlayer.getUsername() + " saiu do mundo!");
            GameServerHandler.broadcastToAll(leaveMsg);

            GameWorld.getInstance().removePlayer(channelId);
            logger.info("{} desconectado e removido do mundo", currentPlayer.getUsername());
        }
        channels.remove(ctx.channel());
        logger.info("Conexao fechada: {}", channelId);
    }

    // Classe interna para detalhes da solicitacao
    private static class FriendRequestDetails {
        String requestId;
        String fromPlayerId;
        String toPlayerId;
        String fromUsername;
    }
}