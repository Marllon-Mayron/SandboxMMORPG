package com.sandbox.server;

import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.*;
import com.common.sandbox.network.packets.AttackInfo;
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
import java.util.ArrayList;
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

            } else if (msg instanceof PlayerStatePacket) {
                if (currentPlayer != null) {
                    handlePlayerState(ctx, (PlayerStatePacket) msg);
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
            } else if (msg instanceof AttackInfo) {
                if (currentPlayer != null) {
                    handleAttack(ctx, (AttackInfo) msg);
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

                // Enviar TODOS os players com HP completo via PlayerStatePacket
                for (Player p : GameWorld.getInstance().getAllPlayers()) {
                    if (!p.getId().equals(player.getId())) {
                        PlayerStatePacket statePacket = new PlayerStatePacket(p);
                        statePacket.fullSync = true;
                        sendPacket(ctx, statePacket);
                    }
                }

                sendPacket(ctx, response);

                // Broadcast do novo jogador com HP completo via PlayerStatePacket
                PlayerStatePacket newPlayerState = new PlayerStatePacket(player);
                newPlayerState.fullSync = true;
                broadcastToAllExcept(newPlayerState, ctx.channel().id().asLongText());

                ChatMessage joinMsg = new ChatMessage(player.getId(), "SISTEMA",
                        player.getUsername() + " entrou no mundo!");
                broadcastToAll(joinMsg);

                sendMapToPlayer(ctx, player);
                sendAllExistingItemsToPlayer(ctx, player);
                sendFriendListToPlayer(ctx, player);
                sendAllExistingPlayers(ctx, player);
                AnimationSyncPacket animSync = new AnimationSyncPacket(AnimationManager.getInstance().getAllProjectileAnimations());
                sendPacket(ctx, animSync);

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

    // ==================== PLAYER STATE UNIFICADO ====================

    private void handlePlayerState(ChannelHandlerContext ctx, PlayerStatePacket packet) {
        try {
            Player player = GameWorld.getInstance().getPlayer(packet.playerId);
            if (player == null) {
                logger.warn("Player not found: {}", packet.playerId);
                return;
            }

            // LOG DO QUE O SERVIDOR RECEBEU
            logger.info("========================================");
            logger.info("SERVER received PlayerState from {}:", player.getUsername());
            logger.info("   HP from client: {}/{}", packet.currentHp, packet.getMaxHp());
            logger.info("   Mana from client: {}/{}", packet.currentMana, packet.getMaxMana());
            logger.info("   Stamina from client: {}/{}", packet.currentStamina, packet.getMaxStamina());
            logger.info("   BaseHP from client: {}, Strength: {}", packet.baseHp, packet.strength);
            logger.info("   Pos from client: ({}, {})", packet.x, packet.y);
            logger.info("   Server current HP: {}/{}", player.getCurrentHp(), player.getMaxHp());
            logger.info("========================================");

            // Validar colisao
            if (ChunkManager.getInstance().isSolid(packet.x, packet.y)) {
                PlayerStatePacket correction = new PlayerStatePacket(player);
                sendPacket(ctx, correction);
                logger.info("   Collision detected, sending correction");
                return;
            }

            // ATUALIZAR POSICAO E DIRECAO
            player.setX(packet.x);
            player.setY(packet.y);
            player.setDirection(packet.direction);

            // ATUALIZAR VALORES BASE
            if (packet.baseHp > 0) {
                player.setBaseHp(packet.baseHp);
            }
            if (packet.baseMana > 0) {
                player.setBaseMana(packet.baseMana);
            }
            if (packet.baseStamina > 0) {
                player.setBaseStamina(packet.baseStamina);
            }

            // ATUALIZAR STATUS ATUAIS
            player.setCurrentHp(packet.currentHp);
            player.setCurrentMana(packet.currentMana);
            player.setCurrentStamina(packet.currentStamina);

            // ATUALIZAR GOLD E EXPERIENCIA
            player.setGold(packet.gold);
            player.setExperience(packet.experience);

            // ATUALIZAR LEVEL
            if (packet.level != player.getLevel()) {
                player.setLevel(packet.level);
            }

            // ATUALIZAR ATRIBUTOS
            if (packet.strength > 0) {
                player.setStrength(packet.strength);
            }
            if (packet.agility > 0) {
                player.setAgility(packet.agility);
            }
            if (packet.wisdom > 0) {
                player.setWisdom(packet.wisdom);
            }

            // VALIDAR SE OS STATUS ATUAIS NAO ULTRAPASSAM O MAX CALCULADO
            if (player.getCurrentHp() > player.getMaxHp()) {
                player.setCurrentHp(player.getMaxHp());
                logger.warn("   Fixed HP overflow for {}: was {}, set to {}",
                        player.getUsername(), packet.currentHp, player.getMaxHp());
            }
            if (player.getCurrentMana() > player.getMaxMana()) {
                player.setCurrentMana(player.getMaxMana());
            }
            if (player.getCurrentStamina() > player.getMaxStamina()) {
                player.setCurrentStamina(player.getMaxStamina());
            }

            logger.info("   After update - Server HP: {}/{}, Mana: {}/{}, Stamina: {}/{}",
                    player.getCurrentHp(), player.getMaxHp(),
                    player.getCurrentMana(), player.getMaxMana(),
                    player.getCurrentStamina(), player.getMaxStamina());

            // BROADCAST - Criar pacote com os valores ATUAIS do servidor
            PlayerStatePacket broadcast = new PlayerStatePacket(player);
            broadcast.currentAttackCooldown = player.getCurrentAttackCooldown();
            broadcastToAll(broadcast);

            // SALVAR NO BANCO
            GameWorld.getInstance().savePlayer(player);

            logger.info("   Broadcast sent to all players and saved to database");

        } catch (Exception e) {
            logger.error("Erro handlePlayerState: {}", e.getMessage(), e);
        }
    }

    private void sendAllExistingPlayers(ChannelHandlerContext ctx, Player newPlayer) {
        Collection<Player> allPlayers = GameWorld.getInstance().getAllPlayers();
        int sentCount = 0;

        for (Player existingPlayer : allPlayers) {
            if (existingPlayer.getId().equals(newPlayer.getId())) {
                continue; // Pular o proprio jogador
            }

            PlayerStatePacket statePacket = new PlayerStatePacket(existingPlayer);
            statePacket.fullSync = true;
            sendPacket(ctx, statePacket);
            sentCount++;

            logger.debug("Enviado jogador existente: {} - HP={}/{}",
                    existingPlayer.getUsername(),
                    existingPlayer.getCurrentHp(),
                    existingPlayer.getMaxHp());
        }

        logger.info("Enviado {} jogadores existentes para {}", sentCount, newPlayer.getUsername());
    }
    // ==================== CHAT ====================

    private void handleChat(ChannelHandlerContext ctx, ChatMessage chatMsg) {
        try {
            if (chatMsg.message == null || chatMsg.message.trim().isEmpty()) return;

            if (chatMsg.message.length() > 500) {
                chatMsg.message = chatMsg.message.substring(0, 500);
            }

            // COMANDO ADMIN PARA SPAWNAR ITEM
            if (chatMsg.message.startsWith("/spawnitem ") && currentPlayer != null) {
                String[] parts = chatMsg.message.split(" ");
                if (parts.length >= 2) {
                    String itemId = parts[1];

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

            if (DatabaseManager.getInstance().areFriends(currentPlayer.getId(), targetPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Voce ja e amigo deste jogador!";
                sendPacket(ctx, response);
                return;
            }

            if (DatabaseManager.getInstance().hasPendingRequest(currentPlayer.getId(), targetPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", targetUsername);
                response.message = "Solicitacao de amizade ja enviada!";
                sendPacket(ctx, response);
                return;
            }

            String requestId = DatabaseManager.getInstance().createFriendRequest(currentPlayer.getId(), targetPlayer.getId());

            if (requestId != null) {
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
            DatabaseManager.FriendRequestDetails details = DatabaseManager.getInstance().getFriendRequestDetails(requestId);

            if (details == null || !details.toPlayerId.equals(currentPlayer.getId())) {
                FriendRequestPacket response = new FriendRequestPacket("ERROR", "");
                response.message = "Solicitacao invalida!";
                sendPacket(ctx, response);
                return;
            }

            boolean success = DatabaseManager.getInstance().acceptFriendRequest(requestId);

            if (success) {
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

    public static Channel getChannelByPlayerId(String playerId) {
        for (Channel channel : getChannels()) {
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

            if (!DatabaseManager.getInstance().areFriends(currentPlayer.getId(), packet.toPlayerId)) {
                PrivateMessagePacket error = new PrivateMessagePacket();
                error.message = "Voce so pode enviar mensagens para amigos!";
                sendPacket(ctx, error);
                return;
            }

            Player targetPlayer = DatabaseManager.getInstance().getPlayerById(packet.toPlayerId);
            if (targetPlayer == null) return;

            packet.fromPlayerId = currentPlayer.getId();
            packet.fromUsername = currentPlayer.getUsername();
            packet.timestamp = System.currentTimeMillis();

            DatabaseManager.getInstance().savePrivateMessage(packet);

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

        boolean added = currentPlayer.getInventory().addItem(def.getId(), 1, def);

        if (added) {
            GameWorld.getInstance().savePlayer(currentPlayer);

            InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
            sendPacket(ctx, invPacket);

            PickupResultPacket result = new PickupResultPacket(true, def.getName(), 1);
            sendPacket(ctx, result);

            ItemDespawnPacket despawn = new ItemDespawnPacket(packet.instanceId);
            broadcastToAll(despawn);

            logger.info("Player {} picked up {} x{}",
                    currentPlayer.getUsername(), def.getName(), 1);
        } else {
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

        currentPlayer.getInventory().removeItem(stack.getItemId(), toDrop);

        float dropX = currentPlayer.getX();
        float dropY = currentPlayer.getY();

        String direction = currentPlayer.getDirection();
        float distance = 100f;

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

        if (ChunkManager.getInstance().isSolid(dropX, dropY)) {
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

        ItemDefinition def = ItemManager.getInstance().getItemDefinition(stack.getItemId());
        if (def != null) {
            ItemManager.getInstance().spawnItem(def.getId(), dropX, dropY, 60);
            logger.info("Player {} dropped {} x{} at ({}, {}) direction: {}",
                    currentPlayer.getUsername(), stack.getItemId(), toDrop, dropX, dropY, direction);
        }

        GameWorld.getInstance().savePlayer(currentPlayer);

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
                        String currentEquipped = currentPlayer.getInventory().getEquipped().get(packet.equipSlot);
                        if (currentEquipped != null && !currentEquipped.isEmpty()) {
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
                String equippedItemId = currentPlayer.getInventory().getEquipped().get(packet.equipSlot);

                if (equippedItemId != null && !equippedItemId.isEmpty()) {
                    currentPlayer.getInventory().unequipItem(packet.equipSlot);

                    ItemDefinition def = ItemManager.getInstance().getItemDefinition(equippedItemId);
                    if (def != null) {
                        currentPlayer.getInventory().addItem(equippedItemId, 1, def);
                        logger.info("Unequipped {} back to inventory", equippedItemId);
                    }
                }
                break;
        }

        GameWorld.getInstance().savePlayer(currentPlayer);

        InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
        sendPacket(ctx, invPacket);
    }

    private void unequipToInventory(String equipSlot) {
        String itemId = currentPlayer.getInventory().getEquipped().get(equipSlot);
        if (itemId == null || itemId.isEmpty()) return;

        currentPlayer.getInventory().unequipItem(equipSlot);

        ItemDefinition def = ItemManager.getInstance().getItemDefinition(itemId);
        if (def != null) {
            currentPlayer.getInventory().addItem(itemId, 1, def);
            logger.info("Unequipped {} back to inventory", itemId);
        }
    }

    private boolean isEquippableCategory(String category) {
        return "weapon".equals(category) || "armor".equals(category) || "equipment".equals(category);
    }

    private void handleAttack(ChannelHandlerContext ctx, AttackInfo attackInfo) {
        if (currentPlayer == null) return;

        // Verificar cooldown
        if (!currentPlayer.canAttack()) {
            return;
        }

        AttackDefinition attackDef = getAttackDefinition(attackInfo.attackId);
        if (attackDef == null) {
            return;
        }

        // Verificar stamina
        if (attackDef.getStaminaCost() > 0 && currentPlayer.getCurrentStamina() < attackDef.getStaminaCost()) {
            return;
        }

        // Consumir stamina
        if (attackDef.getStaminaCost() > 0) {
            currentPlayer.setCurrentStamina(currentPlayer.getCurrentStamina() - (int) attackDef.getStaminaCost());
        }

        // Calcular dano
        updateCombatStatsFromEquipment(currentPlayer);
        CombatStats stats = currentPlayer.getCombatStats();
        int damage = stats.getBaseDamage() + stats.getWeaponDamageBonus() + stats.getStrengthBonus();
        damage = (int) (damage * attackDef.getDamageMultiplier());

        boolean wasCritical = (int)(Math.random() * 100) < stats.getCriticalChance();
        if (wasCritical) {
            damage = damage * stats.getCriticalDamage() / 100;
        }
        damage = Math.max(1, damage);

        // Iniciar cooldown
        currentPlayer.executeAttack();

        // UNIFICADO: SEMPRE PROJÉTIL
        float projectileSpeed = attackDef.isRanged() ? attackDef.getProjectileSpeed() : 3000f;
        float projectileRange = attackDef.getRange();
        String projectileType = attackDef.isRanged() ?
                (attackDef.getProjectileId() != null ? attackDef.getProjectileId() : "arrow") :
                "melee_slash";

        ProjectileManager.getInstance().spawnProjectile(
                currentPlayer, projectileType,
                attackInfo.targetX, attackInfo.targetY,
                damage, wasCritical,
                projectileSpeed, projectileRange, attackDef.isRanged()
        );

        // Broadcast do efeito visual
        AttackBroadcast effect = new AttackBroadcast();
        effect.attackerId = currentPlayer.getId();
        effect.attackerName = currentPlayer.getUsername();
        effect.attackerX = currentPlayer.getX();
        effect.attackerY = currentPlayer.getY();
        effect.targetX = attackInfo.targetX;
        effect.targetY = attackInfo.targetY;
        effect.attackDef = attackDef;
        GameServerHandler.broadcastToAll(effect);

        GameWorld.getInstance().savePlayer(currentPlayer);
    }

    private void updateCombatStatsFromEquipment(Player player) {
        if (player.getCombatStats() == null) {
            player.setCombatStats(new CombatStats());
        }

        int weaponBonus = 0;
        String equippedWeapon = player.getInventory() != null ?
                player.getInventory().getEquipped().get("weapon") : null;

        logger.info("=== EQUIPMENT CHECK ===");
        logger.info("Player: {}", player.getUsername());
        logger.info("Equipped weapon ID: {}", equippedWeapon);

        if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
            ItemDefinition def = ItemManager.getInstance().getItemDefinition(equippedWeapon);
            if (def != null) {
                weaponBonus = def.getDamage();  // ← USA O DANO REAL DO ITEM
                logger.info("Weapon found: {}, Damage: {}", def.getName(), weaponBonus);
            } else {
                logger.warn("Weapon definition NOT FOUND for: {}", equippedWeapon);
            }
        } else {
            logger.info("No weapon equipped, using base damage only");
        }

        player.getCombatStats().setWeaponDamageBonus(weaponBonus);
        player.getCombatStats().setStrengthBonus(player.getStrength() / 2);

        logger.info("Final: BaseDamage={}, WeaponBonus={}, StrengthBonus={}, Total={}",
                player.getCombatStats().getBaseDamage(),
                weaponBonus,
                player.getStrength() / 2,
                player.getCombatStats().getBaseDamage() + weaponBonus + (player.getStrength() / 2));
    }

    private AttackDefinition getAttackDefinition(String attackId) {
        switch (attackId) {
            case "melee_sword": return AttackDefinition.createMeleeSword();
            case "melee_dagger": return AttackDefinition.createMeleeDagger();
            case "ranged_bow": return AttackDefinition.createRangedBow();
            default: return null;
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
}