package com.sandbox.server;

import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.*;
import com.common.sandbox.network.packets.InventoryUpdatePacket;
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
import java.util.Collection;
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
            } else if (msg instanceof PickupItemPacket) {
                handlePickupItem(ctx, (PickupItemPacket) msg);
            } else if (msg instanceof DropItemPacket) {
                handleDropItem(ctx, (DropItemPacket) msg);
            } else if (msg instanceof InventoryUpdatePacket) {
                handleInventoryUpdate(ctx, (InventoryUpdatePacket) msg);
            } else if (msg instanceof AttackRequest) {
                if (currentPlayer != null) {
                    handleAttack(ctx, (AttackRequest) msg);
                }
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

            // ⭐ DEBUG: Verificar quantos itens existem ANTES do login
            ItemManager.getInstance().printAllItems();

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
                    }
                }

                sendPacket(ctx, response);

                ChatMessage joinMsg = new ChatMessage(player.getId(), "SISTEMA",
                        player.getUsername() + " entrou no mundo!");
                broadcastToAll(joinMsg);

                Player movementOnly = new Player();
                movementOnly.setId(player.getId());
                movementOnly.setUsername(player.getUsername());
                movementOnly.setX(player.getX());
                movementOnly.setY(player.getY());
                movementOnly.setDirection(player.getDirection());
                broadcastToAll(new MovementBroadcast(movementOnly));

                sendMapToPlayer(ctx, player);

                // ⭐ ENVIAR TODOS OS ITENS EXISTENTES
                sendAllExistingItemsToPlayer(ctx, player);

                sendFriendListToPlayer(ctx, player);

                // ⭐ NÃO spawnar nenhum item aqui!

            } else {
                logger.warn("Login Falhou - Usuario: {}", request.username);
                LoginResponse response = new LoginResponse(false, "Usuario ou senha invalidos!", null);
                sendPacket(ctx, response);
            }
        } catch (Exception e) {
            logger.error("Erro handleLogin: {}", e.getMessage(), e);
        }
    }

    private void sendAllExistingItemsToPlayer(ChannelHandlerContext ctx, Player player) {
        Collection<GroundItem> allItems = ItemManager.getInstance().getAllItems();

        logger.info("=== SENDING ITEMS TO PLAYER: {} ===", player.getUsername());
        logger.info("Total items in ItemManager: {}", allItems.size());

        // Debug: imprimir todos os itens atuais
        ItemManager.getInstance().printAllItems();

        if (allItems.isEmpty()) {
            logger.warn("⚠️ No items found in ItemManager for player {}", player.getUsername());
            logger.info("Available item definitions: {}", ItemManager.getInstance().getAllItemIds());
            return;
        }

        int sentCount = 0;
        for (GroundItem item : allItems) {
            logger.info("  - Sending item: {} [{}] at ({}, {})",
                    item.getDefinition().getName(),
                    item.getInstanceId().substring(0, 8),
                    item.getX(),
                    item.getY());
            sendPacket(ctx, new ItemSpawnPacket(item));
            sentCount++;
        }

        logger.info("✅ Sent {} items to player {}", sentCount, player.getUsername());
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

    private void sendItemsToPlayer(ChannelHandlerContext ctx, Player player) {
        // Enviar TODOS os itens existentes no mundo
        Collection<GroundItem> allItems = ItemManager.getInstance().getAllItems();

        if (allItems.isEmpty()) {
            logger.info("No items to send to player {}", player.getUsername());
            return;
        }

        int sentCount = 0;
        for (GroundItem item : allItems) {
            sendPacket(ctx, new ItemSpawnPacket(item));
            sentCount++;
        }

        logger.info("Sent {} existing items to player {}", sentCount, player.getUsername());
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

            // Validar colisão
            if (!canMoveTo(request.x, request.y)) {
                MovementBroadcast correction = new MovementBroadcast(player);
                sendPacket(ctx, correction);
                return;
            }

            // ATUALIZAR TUDO - Posição e Status
            player.setX(request.x);
            player.setY(request.y);
            player.setDirection(request.direction);

            // Atualizar status (apenas valores válidos)
            if (request.currentHp > 0 && request.currentHp <= player.getMaxHp()) {
                player.setCurrentHp(request.currentHp);
            }
            if (request.currentMana >= 0 && request.currentMana <= player.getMaxMana()) {
                player.setCurrentMana(request.currentMana);
            }
            if (request.currentStamina >= 0 && request.currentStamina <= player.getMaxStamina()) {
                player.setCurrentStamina(request.currentStamina);
            }
            if (request.currentGold >= 0) {
                player.setGold(request.currentGold);
            }
            if (request.currentExperience >= 0) {
                player.setExperience(request.currentExperience);
            }
            if (request.currentLevel >= 1) {
                int oldLevel = player.getLevel();
                player.setLevel(request.currentLevel);
                if (oldLevel != request.currentLevel) {
                    player.recalculateMaxStats();
                }
            }

            // Salvar periodicamente (a cada 5 segundos já está no updatePlayerPosition)
            GameWorld.getInstance().updatePlayerPosition(request.playerId, request.x, request.y, request.direction);

            // ==================== CRIAR BROADCAST APENAS COM MOVIMENTO ====================
            // IMPORTANTE: Criar um objeto LEVE apenas com dados de movimento
            // para não sobrescrever os status (HP, Mana, Stamina) dos outros clientes
            Player movementOnly = new Player();
            movementOnly.setId(player.getId());
            movementOnly.setUsername(player.getUsername());
            movementOnly.setX(player.getX());
            movementOnly.setY(player.getY());
            movementOnly.setDirection(player.getDirection());
            // NÃO copiar HP, Mana, Stamina, Gold, Experience, Level, etc!

            // Broadcast para TODOS os outros jogadores (exceto quem enviou)
            broadcastToAllExcept(new MovementBroadcast(movementOnly), ctx.channel().id().asLongText());

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

            // ⭐ COMANDO ADMIN PARA SPAWNAR ITEM
            if (chatMsg.message.startsWith("/spawnitem ") && currentPlayer != null) {
                String[] parts = chatMsg.message.split(" ");
                if (parts.length >= 2) {
                    String itemId = parts[1];

                    // Verificar se é admin (você pode adicionar uma flag de admin)
                    // Por enquanto, qualquer um pode usar para teste
                    ItemManager.getInstance().spawnItem(itemId,
                            currentPlayer.getX() + 100,
                            currentPlayer.getY(),
                            60);

                    ChatMessage response = new ChatMessage("SISTEMA", "SISTEMA",
                            "Item " + itemId + " spawnado na sua posição!");
                    sendPacket(ctx, response);
                    return;
                }
            }

            // Comando para listar itens disponíveis
            if (chatMsg.message.equals("/items")) {
                StringBuilder sb = new StringBuilder("Itens disponíveis: ");
                for (String itemId : ItemManager.getInstance().getAllItemIds()) {
                    sb.append(itemId).append(", ");
                }
                ChatMessage response = new ChatMessage("SISTEMA", "SISTEMA", sb.toString());
                sendPacket(ctx, response);
                return;
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

    public static int broadcastToAll(Object packet) {
        int count = 0;
        for (Channel channel : channels) {
            if (channel.isActive()) {
                channel.writeAndFlush(packet);
                count++;
            }
        }
        return count;
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

    //ITEMS

    private void handlePickupItem(ChannelHandlerContext ctx, PickupItemPacket packet) {
        if (currentPlayer == null) return;

        GroundItem groundItem = ItemManager.getInstance().removeItem(packet.instanceId);
        if (groundItem == null) return;

        ItemDefinition def = groundItem.getDefinition();

        // Adicionar ao inventário
        boolean added = currentPlayer.getInventory().addItem(def.getId(), 1, def);

        if (added) {
            // Atualizar banco
            DatabaseManager.getInstance().savePlayerPosition(currentPlayer);

            // Enviar inventário atualizado
            InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
            sendPacket(ctx, invPacket);

            // Enviar resultado
            PickupResultPacket result = new PickupResultPacket(true, def.getName(), 1);
            sendPacket(ctx, result);

            // Broadcast do despawn
            ItemDespawnPacket despawn = new ItemDespawnPacket(packet.instanceId);
            broadcastToAll(despawn);

            logger.info("Player {} picked up {} x{}",
                    currentPlayer.getUsername(), def.getName(), 1);
        } else {
            // Inventário cheio - recolocar o item
            ItemManager.getInstance().respawnItem(groundItem);

            PickupResultPacket result = new PickupResultPacket(false, def.getName(), 1);
            sendPacket(ctx, result);
        }
    }

    private void handleDropItem(ChannelHandlerContext ctx, DropItemPacket packet) {
        if (currentPlayer == null) return;

        ItemStack stack = currentPlayer.getInventory().getSlot(packet.slot);
        if (stack.isEmpty()) return;

        int toDrop = Math.min(packet.quantity, stack.getQuantity());

        // Remover do inventário
        currentPlayer.getInventory().removeItem(stack.getItemId(), toDrop);

        // ⭐ CALCULAR POSIÇÃO A 100 PIXELS DE DISTÂNCIA NA DIREÇÃO DO JOGADOR
        float dropX = currentPlayer.getX();
        float dropY = currentPlayer.getY();

        // Direção baseada na orientação do jogador
        String direction = currentPlayer.getDirection();
        float distance = 100f; // Distância segura

        switch (direction) {
            case "UP":
                dropY += distance;
                break;
            case "DOWN":
                dropY -= distance;
                break;
            case "LEFT":
                dropX -= distance;
                break;
            case "RIGHT":
                dropX += distance;
                break;
            default:
                dropY += distance;
                break;
        }

        // Verificar se a posição não é sólida
        if (ChunkManager.getInstance().isSolid(dropX, dropY)) {
            // Se for sólido, tentar posições alternativas
            float[][] alternatives = {
                    {currentPlayer.getX() + 50, currentPlayer.getY()},
                    {currentPlayer.getX() - 50, currentPlayer.getY()},
                    {currentPlayer.getX(), currentPlayer.getY() + 50},
                    {currentPlayer.getX(), currentPlayer.getY() - 50}
            };

            for (float[] alt : alternatives) {
                if (!ChunkManager.getInstance().isSolid(alt[0], alt[1])) {
                    dropX = alt[0];
                    dropY = alt[1];
                    break;
                }
            }
        }

        // Spawnar no chão
        ItemDefinition def = ItemManager.getInstance().getItemDefinition(stack.getItemId());
        if (def != null) {
            ItemManager.getInstance().spawnItem(def.getId(), dropX, dropY, 60);
            logger.info("Player {} dropped {} x{} at ({}, {}) direction: {}",
                    currentPlayer.getUsername(), stack.getItemId(), toDrop, dropX, dropY, direction);
        }

        // Salvar no banco
        DatabaseManager.getInstance().savePlayerInventory(currentPlayer);

        // Enviar inventário atualizado
        InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
        sendPacket(ctx, invPacket);
    }

    private void handleInventoryUpdate(ChannelHandlerContext ctx, InventoryUpdatePacket packet) {
        if (currentPlayer == null) return;

        switch (packet.action) {
            case "MOVE_ITEM":
                currentPlayer.getInventory().moveItem(packet.slot, packet.targetSlot);
                logger.info("Player {} moved item from slot {} to {}",
                        currentPlayer.getUsername(), packet.slot, packet.targetSlot);
                break;

            case "EQUIP":
                ItemStack stack = currentPlayer.getInventory().getSlot(packet.slot);
                if (stack != null && !stack.isEmpty()) {
                    ItemDefinition def = ItemManager.getInstance().getItemDefinition(stack.getItemId());
                    if (def != null && isEquippableCategory(def.getCategory())) {
                        // Verificar se já tem um item equipado no slot
                        String currentEquipped = currentPlayer.getInventory().getEquipped().get(packet.equipSlot);
                        if (currentEquipped != null && !currentEquipped.isEmpty()) {
                            // Se já tem item equipado, primeiro desequipa ele para o inventário
                            unequipToInventory(packet.equipSlot);
                        }
                        currentPlayer.getInventory().equipItem(packet.slot, packet.equipSlot);
                        logger.info("✅ Player {} equipped {} to slot {}",
                                currentPlayer.getUsername(), stack.getItemId(), packet.equipSlot);
                    } else {
                        logger.warn("❌ Player {} attempted to equip non-equippable item: {} (Category: {})",
                                currentPlayer.getUsername(), stack.getItemId(),
                                def != null ? def.getCategory() : "unknown");
                    }
                }
                break;

            case "UNEQUIP":
                logger.info("=== UNEQUIP DEBUG ===");
                logger.info("Player: {}", currentPlayer.getUsername());
                logger.info("EquipSlot: {}", packet.equipSlot);
                logger.info("Current equipped items: {}", currentPlayer.getInventory().getEquipped());

                String equippedItemId = currentPlayer.getInventory().getEquipped().get(packet.equipSlot);
                logger.info("Equipped item ID: {}", equippedItemId);

                if (equippedItemId != null && !equippedItemId.isEmpty()) {
                    // Remover do equipamento
                    currentPlayer.getInventory().unequipItem(packet.equipSlot);
                    logger.info("Removed from equipment slot: {}", packet.equipSlot);

                    // Adicionar de volta ao inventário
                    ItemDefinition def = ItemManager.getInstance().getItemDefinition(equippedItemId);
                    if (def != null) {
                        boolean added = currentPlayer.getInventory().addItem(equippedItemId, 1, def);
                        logger.info("Added back to inventory: {} - Success: {}", equippedItemId, added);
                    } else {
                        logger.warn("Item definition not found for: {}", equippedItemId);
                    }
                } else {
                    logger.warn("No item equipped in slot: {}", packet.equipSlot);
                }

                logger.info("After unequip - Inventory slots: {}, Equipped: {}",
                        currentPlayer.getInventory().getSlots().size(),
                        currentPlayer.getInventory().getEquipped());
                break;
        }

        // Salvar no banco
        DatabaseManager.getInstance().savePlayerInventory(currentPlayer);

        // Enviar inventário atualizado
        InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
        sendPacket(ctx, invPacket);
    }

    /**
     * Desequipa um item e o coloca de volta no inventário
     */
    private void unequipToInventory(String equipSlot) {
        String itemId = currentPlayer.getInventory().getEquipped().get(equipSlot);
        if (itemId == null || itemId.isEmpty()) return;

        // Remover do equipamento
        currentPlayer.getInventory().unequipItem(equipSlot);

        // Adicionar de volta ao inventário
        ItemDefinition def = ItemManager.getInstance().getItemDefinition(itemId);
        if (def != null) {
            currentPlayer.getInventory().addItem(itemId, 1, def);
            logger.info("Unequipped {} back to inventory", itemId);
        }
    }

    private boolean isEquippableCategory(String category) {
        return "weapon".equals(category) || "armor".equals(category) || "equipment".equals(category);
    }

    private void handleAttack(ChannelHandlerContext ctx, AttackRequest request) {
        CombatManager combatManager = CombatManager.getInstance();
        Collection<Player> allPlayers = GameWorld.getInstance().getAllPlayers();

        // Encontrar alvo mais próximo
        Player target = combatManager.findNearestTarget(currentPlayer, request.attackType, allPlayers);

        if (target == null) {
            logger.debug("{} tried to attack but no target in range", currentPlayer.getUsername());
            return;
        }

        // Processar ataque
        AttackResult result = combatManager.processAttack(currentPlayer, target, request.attackType);

        if (result.isSuccess()) {
            // Broadcast do ataque para todos os jogadores
            AttackBroadcast broadcast = new AttackBroadcast(
                    currentPlayer.getId(),
                    currentPlayer.getUsername(),
                    currentPlayer.getX(),
                    currentPlayer.getY(),
                    result
            );
            broadcastToAll(broadcast);

            // Enviar dano específico para o alvo (atualizar HP)
            DamagePacket damagePacket = new DamagePacket(
                    target.getId(),
                    result.getDamage(),
                    result.isWasCritical(),
                    result.getTargetRemainingHp(),
                    request.attackType
            );

            // Enviar para o alvo
            Channel targetChannel = getChannelByPlayerId(target.getId());
            if (targetChannel != null) {
                targetChannel.writeAndFlush(damagePacket);
            }

            // Enviar atualização de status para o atacante
            sendPacket(ctx, damagePacket);
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