package com.sandbox.server;

import com.common.sandbox.model.combat.AttackDefinition;
import com.common.sandbox.model.item.GroundItem;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.item.ItemStack;
import com.common.sandbox.model.player.CombatStats;
import com.common.sandbox.model.player.Player;
import com.common.sandbox.model.world.MapJSON;
import com.common.sandbox.network.packets.chat.ChatMessage;
import com.common.sandbox.network.packets.chat.PrivateMessageHistoryRequest;
import com.common.sandbox.network.packets.chat.PrivateMessageHistoryResponse;
import com.common.sandbox.network.packets.chat.PrivateMessagePacket;
import com.common.sandbox.network.packets.combat.AnimationSyncPacket;
import com.common.sandbox.network.packets.combat.AttackBroadcast;
import com.common.sandbox.network.packets.combat.AttackInfo;
import com.common.sandbox.network.packets.inventory.*;
import com.common.sandbox.network.packets.auth.LoginRequest;
import com.common.sandbox.network.packets.auth.LoginResponse;
import com.common.sandbox.network.packets.auth.RegisterRequest;
import com.common.sandbox.network.packets.auth.RegisterResponse;
import com.common.sandbox.network.packets.connection.HandshakePacket;
import com.common.sandbox.network.packets.connection.PingPacket;
import com.common.sandbox.network.packets.player.AttributeUpgradePacket;
import com.common.sandbox.network.packets.player.PlayerLeftPacket;
import com.common.sandbox.network.packets.player.PlayerStatePacket;
import com.common.sandbox.network.packets.social.FriendListResponse;
import com.common.sandbox.network.packets.social.FriendRequestPacket;
import com.common.sandbox.network.packets.world.MapLoadRequest;
import com.common.sandbox.network.packets.world.MapLoadResponse;
import com.common.sandbox.network.packets.world.MapSaveRequest;
import com.common.sandbox.network.packets.world.MapSaveResponse;
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
            } else if (msg instanceof AttributeUpgradePacket) {
                if (currentPlayer != null) {
                    handleAttributeUpgrade(ctx, (AttributeUpgradePacket) msg);
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

                // ========== PASSO 1: SALVAR VALORES ORIGINAIS DO BANCO ==========
                int savedCurrentHp = player.getCurrentHp();
                int savedCurrentMana = player.getCurrentMana();
                int savedCurrentStamina = player.getCurrentStamina();
                int savedLevel = player.getLevel();

                logger.info("========================================");
                logger.info("LOGIN SEQUENCE STARTED FOR: {}", request.username);
                logger.info("LOADED FROM DB:");
                logger.info("  - HP: {}/{}", savedCurrentHp, player.getMaxHp());
                logger.info("  - Mana: {}/{}", savedCurrentMana, player.getMaxMana());
                logger.info("  - Stamina: {}/{}", savedCurrentStamina, player.getMaxStamina());
                logger.info("  - Level: {}", savedLevel);
                logger.info("  - Attribute Points: {}", player.getAttributePoints());
                logger.info("  - Equipped items: {}", player.getInventory().getEquipped());
                logger.info("========================================");

                currentPlayer = player;

                // ========== PASSO 2: RESETAR TODOS OS BÔNUS DE EQUIPAMENTO ==========
                // Garantir que começamos do zero (equipmentBonusXxx = 0)
                currentPlayer.resetEquipmentBonuses();
                logger.info("STEP 2 - Equipment bonuses reset");

                // ========== PASSO 3: RECALCULAR BÔNUS DOS EQUIPAMENTOS EQUIPADOS ==========
                recalculateEquipmentBonuses(currentPlayer);
                logger.info("STEP 3 - After equipment recalculation:");
                logger.info("  - EquipmentBonusMaxHp: {}", currentPlayer.getEquipmentBonusMaxHp());
                logger.info("  - MaxHP: {}", currentPlayer.getMaxHp());

                // ========== PASSO 4: APLICAR BÔNUS DE CONJUNTO (SETS) ==========
                SetManager.getInstance().applySetBonuses(currentPlayer);
                logger.info("STEP 4 - After set bonuses:");
                logger.info("  - MaxHP: {}", currentPlayer.getMaxHp());

                // ========== PASSO 5: VALIDAR E CORRIGIR STATUS ATUAIS ==========
                currentPlayer.validateCurrentStats();
                logger.info("STEP 5 - After validation:");
                logger.info("  - MaxHP final: {}", currentPlayer.getMaxHp());

                // ========== PASSO 6: RESTAURAR CURRENT HP/MANA/STAMINA ==========
                // Usar o menor entre o valor salvo e o novo maxHp
                int finalCurrentHp = Math.min(savedCurrentHp, currentPlayer.getMaxHp());
                int finalCurrentMana = Math.min(savedCurrentMana, currentPlayer.getMaxMana());
                int finalCurrentStamina = Math.min(savedCurrentStamina, currentPlayer.getMaxStamina());

                // Se o player morreu antes de deslogar, garantir que não loga com HP 0
                if (finalCurrentHp <= 0) {
                    finalCurrentHp = currentPlayer.getMaxHp();
                    logger.warn("Player logged with 0 HP! Restoring to full: {}", finalCurrentHp);
                }

                currentPlayer.setCurrentHp(finalCurrentHp);
                currentPlayer.setCurrentMana(finalCurrentMana);
                currentPlayer.setCurrentStamina(finalCurrentStamina);

                logger.info("STEP 6 - Restored current values:");
                logger.info("  - CurrentHP: {}/{}", currentPlayer.getCurrentHp(), currentPlayer.getMaxHp());
                logger.info("  - CurrentMana: {}/{}", currentPlayer.getCurrentMana(), currentPlayer.getMaxMana());
                logger.info("  - CurrentStamina: {}/{}", currentPlayer.getCurrentStamina(), currentPlayer.getMaxStamina());

                // ========== PASSO 7: ADICIONAR PLAYER AO MUNDO ==========
                GameWorld.getInstance().addPlayer(currentPlayer, channelId);

                // ========== PASSO 8: CRIAR RESPOSTA DE LOGIN ==========
                LoginResponse response = new LoginResponse(true, "Login bem-sucedido!", currentPlayer);
                response.nearbyPlayers = new java.util.HashMap<>();

                // ========== PASSO 9: ENVIAR TODOS OS OUTROS PLAYERS PARA O NOVO JOGADOR ==========
                int otherPlayersCount = 0;
                for (Player p : GameWorld.getInstance().getAllPlayers()) {
                    if (!p.getId().equals(currentPlayer.getId())) {
                        PlayerStatePacket statePacket = new PlayerStatePacket(p);
                        statePacket.fullSync = true;
                        sendPacket(ctx, statePacket);
                        otherPlayersCount++;
                    }
                }
                logger.info("STEP 7 - Sent {} existing players to {}", otherPlayersCount, currentPlayer.getUsername());

                // ========== PASSO 10: ENVIAR RESPOSTA DE LOGIN ==========
                sendPacket(ctx, response);

                // ========== PASSO 11: BROADCAST DO NOVO JOGADOR ==========
                PlayerStatePacket newPlayerState = new PlayerStatePacket(currentPlayer);
                newPlayerState.fullSync = true;
                broadcastToAllExcept(newPlayerState, ctx.channel().id().asLongText());
                logger.info("STEP 8 - Broadcasted new player to all others");

                // ========== PASSO 12: MENSAGEM DE ENTRADA ==========
                ChatMessage joinMsg = new ChatMessage(currentPlayer.getId(), "SISTEMA",
                        currentPlayer.getUsername() + " entrou no mundo!");
                broadcastToAll(joinMsg);

                // ========== PASSO 13: ENVIAR MAPA ==========
                sendMapToPlayer(ctx, currentPlayer);

                // ========== PASSO 14: ENVIAR TODOS OS ITENS DO CHÃO ==========
                sendAllExistingItemsToPlayer(ctx, currentPlayer);

                // ========== PASSO 15: ENVIAR LISTA DE AMIGOS ==========
                sendFriendListToPlayer(ctx, currentPlayer);

                // ========== PASSO 16: ENVIAR ANIMAÇÕES E DEFINIÇÕES DE ITENS ==========
                AnimationSyncPacket animSync = new AnimationSyncPacket(AnimationManager.getInstance().getAllProjectileAnimations());
                ItemDefinitionSyncPacket itemSync = new ItemDefinitionSyncPacket(ItemManager.getInstance().getAllItemDefinitions());
                sendPacket(ctx, animSync);
                sendPacket(ctx, itemSync);
                logger.info("STEP 9 - Sent animations and item definitions");

                // ========== PASSO 17: LOG FINAL ==========
                logger.info("========================================");
                logger.info("LOGIN COMPLETE FOR: {}", currentPlayer.getUsername());
                logger.info("FINAL STATE:");
                logger.info("  - HP: {}/{}", currentPlayer.getCurrentHp(), currentPlayer.getMaxHp());
                logger.info("  - Mana: {}/{}", currentPlayer.getCurrentMana(), currentPlayer.getMaxMana());
                logger.info("  - Stamina: {}/{}", currentPlayer.getCurrentStamina(), currentPlayer.getMaxStamina());
                logger.info("  - Level: {} | AP: {}", currentPlayer.getLevel(), currentPlayer.getAttributePoints());
                logger.info("  - Physical Power: {}", currentPlayer.getPhysicalPower());
                logger.info("  - Movement Speed: {}", currentPlayer.getMovementSpeed());
                logger.info("========================================");

            } else {
                logger.warn("Login Falhou - Usuario: {}", request.username);
                LoginResponse response = new LoginResponse(false, "Usuário ou senha inválidos!", null);
                sendPacket(ctx, response);
            }
        } catch (Exception e) {
            logger.error("Erro handleLogin: {}", e.getMessage(), e);
            LoginResponse response = new LoginResponse(false, "Erro interno no servidor!", null);
            sendPacket(ctx, response);
        }
    }

    private void sendAllExistingItemsToPlayer(ChannelHandlerContext ctx, Player player) {
        Collection<GroundItem> allItems = ItemManager.getInstance().getAllGroundItems();

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

            // NÃO enviar correção de volta a menos que seja absolutamente necessário
            // O cliente já tem autoridade sobre sua própria posição
            // Apenas validar se não está em área sólida EXTREMA (fora do mapa)
            if (ChunkManager.getInstance().isSolid(packet.x, packet.y)) {
                // Só corrigir se realmente estiver dentro de um bloco sólido
                // Usar um pequeno offset para tentar recuperar
                float correctedX = packet.x;
                float correctedY = packet.y;

                // Tentar encontrar uma posição válida próxima
                float[][] offsets = {{0, 10}, {0, -10}, {10, 0}, {-10, 0}};
                for (float[] offset : offsets) {
                    if (!ChunkManager.getInstance().isSolid(packet.x + offset[0], packet.y + offset[1])) {
                        correctedX = packet.x + offset[0];
                        correctedY = packet.y + offset[1];
                        break;
                    }
                }

                if (correctedX == packet.x && correctedY == packet.y) {
                    logger.warn("Player {} stuck in solid tile at ({},{}), not moving", player.getUsername(), packet.x, packet.y);
                    return;
                }

                packet.x = correctedX;
                packet.y = correctedY;
                logger.info("Corrected player position from ({},{}) to ({},{})",
                        packet.x, packet.y, correctedX, correctedY);
            }

            // Atualizar posição
            player.setX(packet.x);
            player.setY(packet.y);
            player.setDirection(packet.direction);

            // NÃO atualizar HP/Mana/Stamina pelo movimento!
            // O cliente pode enviar valores inconsistentes
            // Apenas atualizar se for fullSync (login, equip, etc)
            if (packet.fullSync) {
                player.setCurrentHp(packet.currentHp);
                player.setCurrentMana(packet.currentMana);
                player.setCurrentStamina(packet.currentStamina);
                player.setGold(packet.gold);
                player.setExperience(packet.experience);

                if (packet.level != player.getLevel()) {
                    player.setLevel(packet.level);
                }
            }

            // Validar limites
            if (player.getCurrentHp() > player.getMaxHp()) {
                player.setCurrentHp(player.getMaxHp());
            }
            if (player.getCurrentMana() > player.getMaxMana()) {
                player.setCurrentMana(player.getMaxMana());
            }
            if (player.getCurrentStamina() > player.getMaxStamina()) {
                player.setCurrentStamina(player.getMaxStamina());
            }

            // Broadcast para OUTROS jogadores (NÃO para o próprio)
            PlayerStatePacket broadcast = new PlayerStatePacket(player);
            broadcast.currentAttackCooldown = player.getCurrentAttackCooldown();

            // IMPORTANTE: NÃO enviar de volta para quem enviou
            broadcastToAllExcept(broadcast, ctx.channel().id().asLongText());

            // Salvar periodicamente (não a cada movimento)
            long now = System.currentTimeMillis();
            Long lastSave = GameWorld.getInstance().getLastSaveTime(player.getId());
            if (lastSave == null || (now - lastSave) >= 5000) {
                GameWorld.getInstance().savePlayer(player);
            }

        } catch (Exception e) {
            logger.error("Erro handlePlayerState: {}", e.getMessage(), e);
        }
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
                weaponBonus = def.getDamage();
                logger.info("Weapon found: {}, Damage: {}", def.getName(), weaponBonus);
            } else {
                logger.warn("Weapon definition NOT FOUND for: {}", equippedWeapon);
            }
        } else {
            logger.info("No weapon equipped, using base damage only");
        }

        player.getCombatStats().setWeaponDamageBonus(weaponBonus);
        // Usar PhysicalPower do player (base + bonus de atributos)
        player.getCombatStats().setStrengthBonus(player.getPhysicalPower() / 2);

        logger.info("Final: PhysicalPower={}, WeaponBonus={}, Total={}",
                player.getPhysicalPower(),
                weaponBonus,
                player.getPhysicalPower() + weaponBonus);
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

        GroundItem groundItem = ItemManager.getInstance().removeGroundItem(packet.instanceId);
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
                        logger.info("Player {} equipped {} to slot {}",
                                currentPlayer.getUsername(), stack.getItemId(), packet.equipSlot);

                        // RECALCULAR BÔNUS DOS EQUIPAMENTOS
                        recalculateEquipmentBonuses(currentPlayer);

                        // ATUALIZAR BÔNUS DE CONJUNTO
                        SetManager.getInstance().refreshSetBonuses(currentPlayer);
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

                        // RECALCULAR BÔNUS DOS EQUIPAMENTOS
                        recalculateEquipmentBonuses(currentPlayer);

                        // ATUALIZAR BÔNUS DE CONJUNTO
                        SetManager.getInstance().refreshSetBonuses(currentPlayer);
                    }
                }
                break;
        }

        GameWorld.getInstance().savePlayer(currentPlayer);

        // Enviar estado atualizado do jogador (com todos os atributos recalculados)
        PlayerStatePacket statePacket = new PlayerStatePacket(currentPlayer);
        statePacket.fullSync = true;
        sendPacket(ctx, statePacket);

        // Enviar inventário atualizado
        InventoryUpdatePacket invPacket = new InventoryUpdatePacket(currentPlayer.getInventory());
        sendPacket(ctx, invPacket);
    }

    /**
     * Recalcula todos os bônus dos equipamentos e aplica ao jogador
     */
    private void recalculateEquipmentBonuses(Player player) {
        if (player == null || player.getInventory() == null) {
            logger.warn("recalculateEquipmentBonuses: player or inventory is null");
            return;
        }

        logger.info("========== RECALCULATING EQUIPMENT BONUSES ==========");
        logger.info("Player: {}", player.getUsername());

        // Mostrar valores ANTES
        logger.info("BEFORE - MaxHP: {}, BonusMaxHp: {}, EquipmentBonusMaxHp: {}",
                player.getMaxHp(), player.getBonusMaxHp(), player.getEquipmentBonusMaxHp());
        logger.info("BEFORE - PhysicalPower: {}, BonusPhysicalPower: {}, EquipmentBonusPhysicalPower: {}",
                player.getPhysicalPower(), player.getBonusPhysicalPower(), player.getEquipmentBonusPhysicalPower());

        // Resetar todos os bônus de equipamento
        player.resetEquipmentBonuses();

        logger.info("AFTER RESET - EquipmentBonusMaxHp: {}", player.getEquipmentBonusMaxHp());

        // Somar bônus de todos os equipamentos equipados
        Map<String, String> equipped = player.getInventory().getEquipped();
        logger.info("Equipped items: {}", equipped);

        for (Map.Entry<String, String> entry : equipped.entrySet()) {
            String slot = entry.getKey();
            String itemId = entry.getValue();
            if (itemId != null && !itemId.isEmpty()) {
                ItemDefinition def = ItemManager.getInstance().getItemDefinition(itemId);
                if (def != null) {
                    logger.info("  Adding bonus from {} (slot: {}): HP+{}, Power+{}",
                            def.getName(), slot, def.getBonusMaxHp(), def.getBonusPhysicalPower());
                    player.addEquipmentBonus(def);
                } else {
                    logger.warn("  Item definition not found for: {}", itemId);
                }
            }
        }

        // Mostrar valores DEPOIS
        logger.info("AFTER ADDING - EquipmentBonusMaxHp: {}", player.getEquipmentBonusMaxHp());
        logger.info("AFTER - MaxHP: {}, PhysicalPower: {}", player.getMaxHp(), player.getPhysicalPower());

        // Validar stats
        player.validateCurrentStats();

        logger.info("FINAL - CurrentHP: {}/{}", player.getCurrentHp(), player.getMaxHp());
        logger.info("======================================================");
    }

    /**
     * Reseta apenas os bônus vindos de equipamentos
     */
    private void resetEquipmentBonuses(Player player) {
        // NOTA: Esta é uma abordagem simples. Idealmente, teríamos campos separados
        // para equipmentBonusMaxHp, equipmentBonusPhysicalPower, etc.
        // Como simplificação, vamos subtrair todos os bônus dos equipamentos atuais

        Map<String, String> equipped = player.getInventory().getEquipped();

        for (String itemId : equipped.values()) {
            if (itemId != null && !itemId.isEmpty()) {
                ItemDefinition def = ItemManager.getInstance().getItemDefinition(itemId);
                if (def != null) {
                    removeEquipmentBonus(player, def);
                }
            }
        }
    }

    /**
     * Aplica os bônus de um item ao jogador
     * CORRIGIDO: Usa os campos equipmentBonusXxx
     */
    private void applyEquipmentBonus(Player player, ItemDefinition def) {
        if (def == null) return;

        // Recursos
        player.setEquipmentBonusMaxHp(player.getEquipmentBonusMaxHp() + def.getBonusMaxHp());
        player.setEquipmentBonusMaxMana(player.getEquipmentBonusMaxMana() + def.getBonusMaxMana());
        player.setEquipmentBonusMaxStamina(player.getEquipmentBonusMaxStamina() + def.getBonusMaxStamina());

        // Regeneração
        player.setEquipmentBonusHpRegen(player.getEquipmentBonusHpRegen() + def.getBonusHpRegen());
        player.setEquipmentBonusManaRegen(player.getEquipmentBonusManaRegen() + def.getBonusManaRegen());
        player.setEquipmentBonusStaminaRegen(player.getEquipmentBonusStaminaRegen() + def.getBonusStaminaRegen());

        // Defesas
        player.setEquipmentBonusPhysicalDefense(player.getEquipmentBonusPhysicalDefense() + def.getBonusPhysicalDefense());
        player.setEquipmentBonusMagicDefense(player.getEquipmentBonusMagicDefense() + def.getBonusMagicDefense());

        // Poder de Dano
        player.setEquipmentBonusPhysicalPower(player.getEquipmentBonusPhysicalPower() + def.getBonusPhysicalPower());
        player.setEquipmentBonusRangedPower(player.getEquipmentBonusRangedPower() + def.getBonusRangedPower());
        player.setEquipmentBonusMagicPower(player.getEquipmentBonusMagicPower() + def.getBonusMagicPower());

        // Chance e Multiplicadores
        player.setEquipmentBonusCriticalChance(player.getEquipmentBonusCriticalChance() + def.getBonusCriticalChance());
        player.setEquipmentBonusCriticalDamage(player.getEquipmentBonusCriticalDamage() + def.getBonusCriticalDamage());
        player.setEquipmentBonusDodgeChance(player.getEquipmentBonusDodgeChance() + def.getBonusDodgeChance());

        // Velocidades
        player.setEquipmentBonusAttackSpeed(player.getEquipmentBonusAttackSpeed() + def.getBonusAttackSpeed());
        player.setEquipmentBonusMovementSpeed(player.getEquipmentBonusMovementSpeed() + def.getBonusMovementSpeed());

        // Utilidades
        player.setEquipmentBonusCooldownReduction(player.getEquipmentBonusCooldownReduction() + def.getBonusCooldownReduction());
        player.setEquipmentBonusLifeSteal(player.getEquipmentBonusLifeSteal() + def.getBonusLifeSteal());
        player.setEquipmentBonusManaSteal(player.getEquipmentBonusManaSteal() + def.getBonusManaSteal());
        player.setEquipmentBonusTenacity(player.getEquipmentBonusTenacity() + def.getBonusTenacity());

        // Sorte
        player.setEquipmentBonusLuck(player.getEquipmentBonusLuck() + def.getBonusLuck());

        // Resistências Elementais
        player.setEquipmentBonusFireResistance(player.getEquipmentBonusFireResistance() + def.getBonusFireResistance());
        player.setEquipmentBonusIceResistance(player.getEquipmentBonusIceResistance() + def.getBonusIceResistance());
        player.setEquipmentBonusLightningResistance(player.getEquipmentBonusLightningResistance() + def.getBonusLightningResistance());
        player.setEquipmentBonusPoisonResistance(player.getEquipmentBonusPoisonResistance() + def.getBonusPoisonResistance());
        player.setEquipmentBonusHolyResistance(player.getEquipmentBonusHolyResistance() + def.getBonusHolyResistance());
        player.setEquipmentBonusDarkResistance(player.getEquipmentBonusDarkResistance() + def.getBonusDarkResistance());

        logger.debug("Applied equipment bonus: {} - +{} HP, +{} Physical Power",
                def.getName(), def.getBonusMaxHp(), def.getBonusPhysicalPower());
    }

    /**
     * Remove os bônus de um item do jogador
     * CORRIGIDO: Usa os campos equipmentBonusXxx
     */
    private void removeEquipmentBonus(Player player, ItemDefinition def) {
        if (def == null) return;

        // Recursos
        player.setEquipmentBonusMaxHp(player.getEquipmentBonusMaxHp() - def.getBonusMaxHp());
        player.setEquipmentBonusMaxMana(player.getEquipmentBonusMaxMana() - def.getBonusMaxMana());
        player.setEquipmentBonusMaxStamina(player.getEquipmentBonusMaxStamina() - def.getBonusMaxStamina());

        // Regeneração
        player.setEquipmentBonusHpRegen(player.getEquipmentBonusHpRegen() - def.getBonusHpRegen());
        player.setEquipmentBonusManaRegen(player.getEquipmentBonusManaRegen() - def.getBonusManaRegen());
        player.setEquipmentBonusStaminaRegen(player.getEquipmentBonusStaminaRegen() - def.getBonusStaminaRegen());

        // Defesas
        player.setEquipmentBonusPhysicalDefense(player.getEquipmentBonusPhysicalDefense() - def.getBonusPhysicalDefense());
        player.setEquipmentBonusMagicDefense(player.getEquipmentBonusMagicDefense() - def.getBonusMagicDefense());

        // Poder de Dano
        player.setEquipmentBonusPhysicalPower(player.getEquipmentBonusPhysicalPower() - def.getBonusPhysicalPower());
        player.setEquipmentBonusRangedPower(player.getEquipmentBonusRangedPower() - def.getBonusRangedPower());
        player.setEquipmentBonusMagicPower(player.getEquipmentBonusMagicPower() - def.getBonusMagicPower());

        // Chance e Multiplicadores
        player.setEquipmentBonusCriticalChance(player.getEquipmentBonusCriticalChance() - def.getBonusCriticalChance());
        player.setEquipmentBonusCriticalDamage(player.getEquipmentBonusCriticalDamage() - def.getBonusCriticalDamage());
        player.setEquipmentBonusDodgeChance(player.getEquipmentBonusDodgeChance() - def.getBonusDodgeChance());

        // Velocidades
        player.setEquipmentBonusAttackSpeed(player.getEquipmentBonusAttackSpeed() - def.getBonusAttackSpeed());
        player.setEquipmentBonusMovementSpeed(player.getEquipmentBonusMovementSpeed() - def.getBonusMovementSpeed());

        // Utilidades
        player.setEquipmentBonusCooldownReduction(player.getEquipmentBonusCooldownReduction() - def.getBonusCooldownReduction());
        player.setEquipmentBonusLifeSteal(player.getEquipmentBonusLifeSteal() - def.getBonusLifeSteal());
        player.setEquipmentBonusManaSteal(player.getEquipmentBonusManaSteal() - def.getBonusManaSteal());
        player.setEquipmentBonusTenacity(player.getEquipmentBonusTenacity() - def.getBonusTenacity());

        // Sorte
        player.setEquipmentBonusLuck(player.getEquipmentBonusLuck() - def.getBonusLuck());

        // Resistências Elementais
        player.setEquipmentBonusFireResistance(player.getEquipmentBonusFireResistance() - def.getBonusFireResistance());
        player.setEquipmentBonusIceResistance(player.getEquipmentBonusIceResistance() - def.getBonusIceResistance());
        player.setEquipmentBonusLightningResistance(player.getEquipmentBonusLightningResistance() - def.getBonusLightningResistance());
        player.setEquipmentBonusPoisonResistance(player.getEquipmentBonusPoisonResistance() - def.getBonusPoisonResistance());
        player.setEquipmentBonusHolyResistance(player.getEquipmentBonusHolyResistance() - def.getBonusHolyResistance());
        player.setEquipmentBonusDarkResistance(player.getEquipmentBonusDarkResistance() - def.getBonusDarkResistance());

        logger.debug("Removed equipment bonus: {}", def.getName());
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
        return "weapon".equals(category) || "armor".equals(category) || "accessory".equals(category) || "equipment".equals(category);
    }

    private void handleAttributeUpgrade(ChannelHandlerContext ctx, AttributeUpgradePacket packet) {
        if (currentPlayer == null || packet.upgrades == null) return;

        logger.info("Received attribute upgrades from {}: {} upgrades",
                currentPlayer.getUsername(), packet.upgrades.size());

        int totalPointsUsed = 0;

        for (Map.Entry<String, Integer> entry : packet.upgrades.entrySet()) {
            String attributeId = entry.getKey();
            int value = entry.getValue();

            totalPointsUsed += getPointCostForAttribute(attributeId, value);
            applyUpgradeToPlayer(attributeId, value);
        }

        if (totalPointsUsed > currentPlayer.getAttributePoints()) {
            logger.warn("Player {} tried to use more points than available!", currentPlayer.getUsername());
            return;
        }

        currentPlayer.setAttributePoints(currentPlayer.getAttributePoints() - totalPointsUsed);

        // Validar stats após aplicar upgrades
        currentPlayer.validateCurrentStats();

        // Salvar no banco
        DatabaseManager.getInstance().savePlayerAsync(currentPlayer);

        // Enviar estado atualizado para o cliente (APENAS para quem enviou)
        PlayerStatePacket statePacket = new PlayerStatePacket(currentPlayer);
        statePacket.fullSync = true;
        sendPacket(ctx, statePacket);

        // Também broadcast para outros players (para atualizar vida máxima visível)
        broadcastToAll(statePacket);

        logger.info("Applied {} upgrades to {}, {} points remaining. New MaxHP: {}",
                packet.upgrades.size(), currentPlayer.getUsername(),
                currentPlayer.getAttributePoints(), currentPlayer.getMaxHp());
    }

    private int getPointCostForAttribute(String attributeId, int value) {
        switch (attributeId) {
            case "max_hp": return value / 10;
            case "max_mana": return value / 10;
            case "max_stamina": return value / 10;
            case "hp_regen": return value;
            case "mana_regen": return value;
            case "stamina_regen": return value;
            case "physical_defense": return value / 5;
            case "magic_defense": return value / 5;
            case "physical_power": return value / 3;
            case "ranged_power": return value / 3;
            case "magic_power": return value / 3;
            case "critical_chance": return value;
            case "critical_damage": return value / 5;
            case "dodge_chance": return value;
            case "attack_speed": return value / 2;
            case "movement_speed": return value / 10;
            case "cooldown_reduction": return value;
            case "life_steal": return value;
            case "mana_steal": return value;
            case "tenacity": return value;
            case "luck": return value / 5;
            case "fire_resistance": return value / 5;
            case "ice_resistance": return value / 5;
            case "lightning_resistance": return value / 5;
            case "poison_resistance": return value / 5;
            case "holy_resistance": return value / 5;
            case "dark_resistance": return value / 5;
            default: return value;
        }
    }

    private void applyUpgradeToPlayer(String attributeId, int value) {
        switch (attributeId) {
            case "max_hp":
                currentPlayer.setBonusMaxHp(currentPlayer.getBonusMaxHp() + value);
                currentPlayer.setCurrentHp(currentPlayer.getMaxHp());
                break;
            case "max_mana":
                currentPlayer.setBonusMaxMana(currentPlayer.getBonusMaxMana() + value);
                currentPlayer.setCurrentMana(currentPlayer.getMaxMana());
                break;
            case "max_stamina":
                currentPlayer.setBonusMaxStamina(currentPlayer.getBonusMaxStamina() + value);
                currentPlayer.setCurrentStamina(currentPlayer.getMaxStamina());
                break;
            case "hp_regen":
                currentPlayer.setBonusHpRegen(currentPlayer.getBonusHpRegen() + value);
                break;
            case "mana_regen":
                currentPlayer.setBonusManaRegen(currentPlayer.getBonusManaRegen() + value);
                break;
            case "stamina_regen":
                currentPlayer.setBonusStaminaRegen(currentPlayer.getBonusStaminaRegen() + value);
                break;
            case "physical_defense":
                currentPlayer.setBonusPhysicalDefense(currentPlayer.getBonusPhysicalDefense() + value);
                break;
            case "magic_defense":
                currentPlayer.setBonusMagicDefense(currentPlayer.getBonusMagicDefense() + value);
                break;
            case "physical_power":
                currentPlayer.setBonusPhysicalPower(currentPlayer.getBonusPhysicalPower() + value);
                break;
            case "ranged_power":
                currentPlayer.setBonusRangedPower(currentPlayer.getBonusRangedPower() + value);
                break;
            case "magic_power":
                currentPlayer.setBonusMagicPower(currentPlayer.getBonusMagicPower() + value);
                break;
            case "critical_chance":
                currentPlayer.setBonusCriticalChance(currentPlayer.getBonusCriticalChance() + (value / 100f));
                break;
            case "critical_damage":
                currentPlayer.setBonusCriticalDamage(currentPlayer.getBonusCriticalDamage() + (value / 100f));
                break;
            case "dodge_chance":
                currentPlayer.setBonusDodgeChance(currentPlayer.getBonusDodgeChance() + (value / 100f));
                break;
            case "attack_speed":
                currentPlayer.setBonusAttackSpeed(currentPlayer.getBonusAttackSpeed() + (value / 100f));
                break;
            case "movement_speed":
                currentPlayer.setBonusMovementSpeed(currentPlayer.getBonusMovementSpeed() + value);
                break;
            case "cooldown_reduction":
                currentPlayer.setBonusCooldownReduction(currentPlayer.getBonusCooldownReduction() + (value / 100f));
                break;
            case "life_steal":
                currentPlayer.setBonusLifeSteal(currentPlayer.getBonusLifeSteal() + (value / 100f));
                break;
            case "mana_steal":
                currentPlayer.setBonusManaSteal(currentPlayer.getBonusManaSteal() + (value / 100f));
                break;
            case "tenacity":
                currentPlayer.setBonusTenacity(currentPlayer.getBonusTenacity() + (value / 100f));
                break;
            case "luck":
                currentPlayer.setBonusLuck(currentPlayer.getBonusLuck() + value);
                break;
            case "fire_resistance":
                currentPlayer.setBonusFireResistance(currentPlayer.getBonusFireResistance() + value);
                break;
            case "ice_resistance":
                currentPlayer.setBonusIceResistance(currentPlayer.getBonusIceResistance() + value);
                break;
            case "lightning_resistance":
                currentPlayer.setBonusLightningResistance(currentPlayer.getBonusLightningResistance() + value);
                break;
            case "poison_resistance":
                currentPlayer.setBonusPoisonResistance(currentPlayer.getBonusPoisonResistance() + value);
                break;
            case "holy_resistance":
                currentPlayer.setBonusHolyResistance(currentPlayer.getBonusHolyResistance() + value);
                break;
            case "dark_resistance":
                currentPlayer.setBonusDarkResistance(currentPlayer.getBonusDarkResistance() + value);
                break;
        }

        currentPlayer.validateCurrentStats();
    }

    private void handleAttack(ChannelHandlerContext ctx, AttackInfo attackInfo) {
        if (currentPlayer == null) return;

        // Verificar cooldown
        if (!currentPlayer.canAttack()) {
            // Enviar mensagem de erro para o cliente
            ChatMessage cooldownMsg = new ChatMessage("SISTEMA", "SISTEMA",
                    "Arma recarregando! Aguarde " +
                            String.format("%.1f", getRemainingCooldown(currentPlayer)) + "s");
            sendPacket(ctx, cooldownMsg);
            return;
        }

        AttackDefinition attackDef = getAttackDefinition(attackInfo.attackId);
        if (attackDef == null) {
            logger.warn("Unknown attack definition: {}", attackInfo.attackId);
            return;
        }

        // Verificar stamina
        if (attackDef.getStaminaCost() > 0 && currentPlayer.getCurrentStamina() < attackDef.getStaminaCost()) {
            ChatMessage staminaMsg = new ChatMessage("SISTEMA", "SISTEMA",
                    "Stamina insuficiente! Necessário: " + (int)attackDef.getStaminaCost());
            sendPacket(ctx, staminaMsg);
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

        // Log do ataque
        logger.info("⚔️ {} attacking with {} | Damage: {}{} | Stamina: {}/{}",
                currentPlayer.getUsername(), attackDef.getName(), damage,
                wasCritical ? " (CRITICAL!)" : "",
                currentPlayer.getCurrentStamina(), currentPlayer.getMaxStamina());

        // Iniciar cooldown (seta lastAttackTime)
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

        // Sincronizar estado do jogador com o cliente (cooldown, stamina, etc.)
        PlayerStatePacket stateUpdate = new PlayerStatePacket(currentPlayer);
        stateUpdate.currentAttackCooldown = currentPlayer.getCurrentAttackCooldown();
        sendPacket(ctx, stateUpdate);

        // Salvar no banco
        GameWorld.getInstance().savePlayer(currentPlayer);
    }

    private float getRemainingCooldown(Player player) {
        long now = System.currentTimeMillis();
        long elapsed = now - player.getLastAttackTime();
        float cooldownSecs = player.getCurrentAttackCooldown();
        long cooldownMillis = (long)(cooldownSecs * 1000);
        float remaining = (cooldownMillis - elapsed) / 1000f;
        return Math.max(0, remaining);
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