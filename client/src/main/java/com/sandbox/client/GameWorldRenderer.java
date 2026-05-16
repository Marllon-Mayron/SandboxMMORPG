package com.sandbox.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.common.sandbox.model.combat.AttackDefinition;
import com.common.sandbox.model.combat.AttackResult;
import com.common.sandbox.model.enums.AttackHitboxType;
import com.common.sandbox.model.item.GroundItem;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.item.ItemStack;
import com.common.sandbox.model.player.Player;
import com.common.sandbox.model.world.Chunk;
import com.common.sandbox.model.world.MapJSON;
import com.common.sandbox.model.world.WorldTile;
import com.common.sandbox.network.packets.chat.ChatMessage;
import com.common.sandbox.network.packets.chat.PrivateMessageHistoryResponse;
import com.common.sandbox.network.packets.chat.PrivateMessagePacket;
import com.common.sandbox.network.packets.combat.*;
import com.common.sandbox.network.packets.inventory.*;
import com.common.sandbox.network.packets.player.PlayerLeftPacket;
import com.common.sandbox.network.packets.player.PlayerStatePacket;
import com.common.sandbox.network.packets.social.FriendListResponse;
import com.common.sandbox.network.packets.social.FriendRequestPacket;
import com.sandbox.client.editor.MapEditorScreen;
import com.sandbox.client.input.PlayerInputManager;
import com.sandbox.client.renderer.AttackHitboxRenderer;
import com.sandbox.client.renderer.ItemRenderer;
import com.sandbox.client.renderer.animation.ProjectileAnimationRenderer;
import com.sandbox.client.renderer.effects.AttackEffectManager;
import com.sandbox.client.ui.PlayerUI;
import com.sandbox.client.camera.GameCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GameWorldRenderer implements Screen {
    private static final Logger logger = LoggerFactory.getLogger(GameWorldRenderer.class);
    private static final int TILE_SIZE = 32;
    private static final int WORLD_TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 32;
    private static final float PLAYER_SIZE = 48;
    private static final float PICKUP_RANGE = 48f;

    private final SandboxClient game;
    private final boolean adminMode;
    private GameCamera gameCamera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Player currentPlayer;
    private final Map<String, Player> otherPlayers;
    private final Map<String, Chunk> loadedChunks;

    private final Map<String, Player> interpolatedPlayers = new ConcurrentHashMap<>();
    private final Map<String, Long> lastUpdateTime = new ConcurrentHashMap<>();

    private final Map<String, Texture> spritesheets;
    private final Map<String, TextureRegion[][]> spritesheetRegions;

    private PlayerUI playerUI;
    private boolean initialized = false;
    private boolean spritesheetsLoaded = false;
    private Set<String> pendingSpritesheets = new HashSet<>();
    private MapJSON pendingMap = null;
    private final Map<String, Player> initialNearbyPlayers;
    private final StringBuilder chatDisplay;

    // ==================== SISTEMA DE MOVIMENTO ====================
    private PlayerInputManager playerInputManager;
    private boolean isDashing = false;
    private float dashTimer = 0;
    private Vector2 dashStartPos;
    private Vector2 dashDirection;

    // Rate limiting para movimentos
    private long lastMovementSendTime = 0;
    private static final long MOVEMENT_SEND_INTERVAL_MS = 50;
    private long lastStatusSyncTime = 0;
    private static final long STATUS_SYNC_INTERVAL_MS = 10000;

    // ==================== SISTEMA DE ITENS ====================
    private ItemRenderer itemRenderer;
    private final Map<String, GroundItem> localGroundItems = new ConcurrentHashMap<>();
    private long lastPickupCheck = 0;
    private static final long PICKUP_CHECK_INTERVAL_MS = 500;
    private final Map<String, ItemDefinition> itemDefinitionCache = new ConcurrentHashMap<>();

    // ==================== SISTEMA DE COMBATE ====================
    private long lastAttackTime = 0;
    private AttackHitboxRenderer attackHitboxRenderer;
    private AttackEffectManager attackEffectManager;
    private ProjectileAnimationRenderer projectileAnimRenderer;

    public GameWorldRenderer(SandboxClient game, boolean adminMode, Map<String, Player> nearbyPlayers) {
        this.game = game;
        this.adminMode = adminMode;
        this.initialNearbyPlayers = nearbyPlayers != null ? nearbyPlayers : new HashMap<>();
        this.otherPlayers = new ConcurrentHashMap<>();
        this.loadedChunks = new ConcurrentHashMap<>();
        this.spritesheets = new HashMap<>();
        this.spritesheetRegions = new HashMap<>();
        this.chatDisplay = new StringBuilder();
        this.dashStartPos = new Vector2();
        this.dashDirection = new Vector2();

        logger.info("GameWorldRenderer created with {} initial nearby players", this.initialNearbyPlayers.size());

        setupCallbacks();
    }

    public void init() {
        // Inicializa a camera suave
        gameCamera = new GameCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        gameCamera.setFollowSpeed(6.0f);
        gameCamera.setDeadZone(80, 80);

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 18;
            parameter.borderWidth = 1;
            parameter.borderColor = Color.BLACK;
            parameter.color = Color.WHITE;
            font = generator.generateFont(parameter);
            generator.dispose();
        } catch (Exception e) {
            font = new BitmapFont();
            font.getData().setScale(1.0f);
        }

        createPlayerUI();
        setupPlayerInput();

        // Carregar spritesheet dos itens
        loadItemSpritesheet();

        itemRenderer = new ItemRenderer();
        attackHitboxRenderer = new AttackHitboxRenderer();
        attackEffectManager = new AttackEffectManager();
        projectileAnimRenderer = new ProjectileAnimationRenderer();

        if (initialNearbyPlayers != null && !initialNearbyPlayers.isEmpty()) {
            logger.info("Adding {} initial nearby players to world", initialNearbyPlayers.size());
            for (Player player : initialNearbyPlayers.values()) {
                if (currentPlayer == null || !player.getId().equals(currentPlayer.getId())) {
                    otherPlayers.put(player.getId(), player);
                    logger.debug("Initial player loaded: {} at ({}, {})",
                            player.getUsername(), player.getX(), player.getY());
                }
            }
        }

        initialized = true;
        game.getNetworkClient().requestMapLoad("11111111-1111-1111-1111-111111111111");

        if (currentPlayer != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.resetPosition();
        }
    }

    private void loadItemSpritesheet() {
        String path = "itens/spritesheet_itens.png";

        String[] possiblePaths = {
                path,
                "assets/" + path,
                "client/assets/" + path,
                "../client/assets/" + path
        };

        FileHandle file = null;
        String loadedPath = null;

        for (String tryPath : possiblePaths) {
            file = Gdx.files.internal(tryPath);
            if (file.exists()) {
                loadedPath = tryPath;
                logger.info("Found item spritesheet at: {}", tryPath);
                break;
            }
        }

        if (loadedPath == null) {
            logger.error("❌ Item spritesheet not found in any path!");
            createPlaceholderSpritesheet(path);
            return;
        }

        if (spritesheets.containsKey(loadedPath)) {
            logger.info("Item spritesheet already loaded: {}", loadedPath);
            return;
        }

        try {
            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
            spritesheets.put(loadedPath, texture);

            int cols = texture.getWidth() / TILE_SIZE;
            int rows = texture.getHeight() / TILE_SIZE;
            TextureRegion[][] regions = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
            spritesheetRegions.put(loadedPath, regions);

            logger.info("✅ Loaded item spritesheet: {} ({}x{} tiles, {}x{} pixels)",
                    loadedPath, cols, rows, texture.getWidth(), texture.getHeight());

            // Após carregar o spritesheet, registrar texturas dos itens que já estão no cache
            if (playerUI != null && !itemDefinitionCache.isEmpty()) {
                logger.info("Registering textures for {} cached item definitions", itemDefinitionCache.size());
                for (ItemDefinition def : itemDefinitionCache.values()) {
                    registerItemTextureForDefinition(def);
                }
            }

            // Se o jogador já tem inventário, atualizar as texturas
            if (currentPlayer != null && currentPlayer.getInventory() != null && playerUI != null) {
                refreshInventoryTextures();
            }

        } catch (Exception e) {
            logger.error("Failed to load item spritesheet: {}", loadedPath, e);
        }
    }

    private void createPlaceholderSpritesheet(String path) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.8f, 0.2f, 0.2f, 1f);
        pixmap.fillRectangle(0, 32, 32, 32);
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
        pixmap.fillRectangle(0, 0, 32, 32);
        pixmap.setColor(0.2f, 0.2f, 0.8f, 1f);
        pixmap.fillRectangle(32, 0, 32, 32);
        pixmap.setColor(0.3f, 0.3f, 0.3f, 1f);
        pixmap.fillRectangle(32, 32, 32, 32);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        spritesheets.put(path, texture);
    }

    private void setupCallbacks() {
        logger.info("Setting up callbacks for GameWorldRenderer");

        game.getNetworkClient().setPlayerStateCallback(this::onPlayerState);
        game.getNetworkClient().setPlayerLeftCallback(this::onPlayerLeft);
        game.getNetworkClient().setChatCallback(this::onChatMessage);
        game.getNetworkClient().setChunkCallback(this::onChunkReceived);
        game.getNetworkClient().setMapLoadCallback(response -> {
            if (response.success && response.mapJson != null) {
                logger.info("Map loaded, processing...");
                pendingMap = response.mapJson;
                spritesheetsLoaded = false;

                Gdx.app.postRunnable(() -> {
                    Set<String> requiredPaths = collectSpritesheetPaths(response.mapJson);
                    loadRequiredSpritesheets(requiredPaths, response.mapJson);
                });
            }
        });

        // Sistema de Amigos
        game.getNetworkClient().setFriendListCallback(this::onFriendListResponse);
        game.getNetworkClient().setFriendRequestCallback(this::onFriendRequestResponse);
        game.getNetworkClient().setPrivateMessageCallback(this::onPrivateMessage);
        game.getNetworkClient().setPrivateMessageHistoryCallback(this::onPrivateMessageHistory);

        // Sistema de Itens
        game.getNetworkClient().setItemSpawnCallback(this::onItemSpawn);
        game.getNetworkClient().setItemDespawnCallback(this::onItemDespawn);

        // Sistema de Inventário
        game.getNetworkClient().setInventoryCallback(this::onInventoryUpdate);
        game.getNetworkClient().setPickupResultCallback(this::onPickupResult);

        // Sistema de Combate
        game.getNetworkClient().setAttackBroadcastCallback(this::onAttackBroadcast);
        game.getNetworkClient().setDamagePacketCallback(this::onDamagePacket);
        game.getNetworkClient().setProjectileStateCallback(this::onProjectileState);

        //Sistema de Animações
        game.getNetworkClient().setAnimationSyncCallback(this::onAnimationSync);
        logger.info("Callbacks configured");
    }

    // ==================== CALLBACKS DE COMBATE ====================

    public void onAttackBroadcast(AttackBroadcast broadcast) {
        Gdx.app.postRunnable(() -> {
            // SEMPRE mostrar hitbox em debug mode, independente de acertar ou não
            if (attackHitboxRenderer != null && broadcast.attackDef != null) {
                float targetX = broadcast.targetX;
                float targetY = broadcast.targetY;

                if (targetX == 0 && targetY == 0 && broadcast.results != null && !broadcast.results.isEmpty()) {
                    AttackResult firstResult = broadcast.results.get(0);
                    float angle = (float) Math.atan2(firstResult.getKnockbackY(), firstResult.getKnockbackX());
                    targetX = broadcast.attackerX + (float) Math.cos(angle) * 50;
                    targetY = broadcast.attackerY + (float) Math.sin(angle) * 50;
                }

                // Usar hitboxDuration do attackDef (3 segundos para teste)
                float hitboxDuration = broadcast.attackDef.getHitboxDuration();
                if (hitboxDuration <= 0) hitboxDuration = 0.5f;

                attackHitboxRenderer.showHitbox(
                        broadcast.attackDef,
                        broadcast.attackerX,
                        broadcast.attackerY,
                        targetX,
                        targetY,
                        hitboxDuration
                );
            }

            // Mostrar efeitos visuais apenas se acertou
            if (broadcast.results != null && !broadcast.results.isEmpty() && broadcast.results.get(0).isSuccess()) {
                if (attackEffectManager != null) {
                    attackEffectManager.addAttackEffect(broadcast);
                }
            }

            // Feedback do dano...
            if (currentPlayer != null && broadcast != null && broadcast.attackerId != null &&
                    broadcast.attackerId.equals(currentPlayer.getId())) {
                if (playerUI != null && broadcast.results != null && !broadcast.results.isEmpty()) {
                    AttackResult result = broadcast.results.get(0);
                    if (result.isSuccess()) {
                        String critMsg = result.isWasCritical() ? " (CRITICAL!)" : "";
                        playerUI.addChatMessage("⚔️ You dealt " + result.getDamage() +
                                " damage to " + result.getTargetName() + critMsg);
                    }
                }
            }

            if (currentPlayer != null && broadcast != null && broadcast.results != null) {
                for (AttackResult result : broadcast.results) {
                    if (result.getTargetId() != null && result.getTargetId().equals(currentPlayer.getId())) {
                        if (playerUI != null) {
                            playerUI.addChatMessage("❤️ You took " + result.getDamage() +
                                    " damage from " + broadcast.attackerName);
                            playerUI.setHealth(currentPlayer.getHpPercentage() * 100);
                        }
                        break;
                    }
                }
            }
        });
    }

    public void onDamagePacket(DamagePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (currentPlayer != null && packet.getTargetId().equals(currentPlayer.getId())) {
                // Eu tomei dano
                currentPlayer.setCurrentHp(packet.getNewHp());
                if (playerUI != null) {
                    playerUI.setHealth(currentPlayer.getHpPercentage() * 100);
                    playerUI.addChatMessage("❤️ You took " + packet.getDamage() + " damage! HP: " +
                            currentPlayer.getCurrentHp() + "/" + currentPlayer.getMaxHp());
                }
                logger.info("I took damage! New HP: {}/{}", currentPlayer.getCurrentHp(), currentPlayer.getMaxHp());
            } else {
                // Outro jogador tomou dano
                Player target = otherPlayers.get(packet.getTargetId());
                if (target != null) {
                    target.setCurrentHp(packet.getNewHp());
                    logger.info("Player {} took damage! New HP: {}/{}",
                            target.getUsername(), target.getCurrentHp(), target.getMaxHp());
                }
            }
        });
    }

    // ==================== CALLBACKS EXISTENTES ====================

    public void onPlayerState(PlayerStatePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (packet == null) return;

            boolean isSelf = (currentPlayer != null && packet.playerId.equals(currentPlayer.getId()));

            if (isSelf) {
                // ATUALIZAR APENAS POSICAO E DIRECAO
                currentPlayer.setX(packet.x);
                currentPlayer.setY(packet.y);
                currentPlayer.setDirection(packet.direction);

                // NAO ATUALIZAR OUTROS CAMPOS PARA NAO SOBRESCREVER OS UPGRADES LOCAIS
                // O servidor pode ter valores desatualizados
            } else {
                // Outro jogador
                Player existing = otherPlayers.get(packet.playerId);

                if (existing != null) {
                    existing.setX(packet.x);
                    existing.setY(packet.y);
                    existing.setDirection(packet.direction);
                    existing.setCurrentHp(packet.currentHp);
                    existing.setCurrentMana(packet.currentMana);
                    existing.setCurrentStamina(packet.currentStamina);
                    existing.setLevel(packet.level);
                    existing.setGold(packet.gold);
                    existing.setExperience(packet.experience);
                    existing.setAttributePoints(packet.attributePoints);
                } else {
                    Player newPlayer = new Player();
                    newPlayer.setId(packet.playerId);
                    newPlayer.setUsername(packet.username);
                    newPlayer.setX(packet.x);
                    newPlayer.setY(packet.y);
                    newPlayer.setDirection(packet.direction);
                    newPlayer.setCurrentHp(packet.currentHp);
                    newPlayer.setCurrentMana(packet.currentMana);
                    newPlayer.setCurrentStamina(packet.currentStamina);
                    newPlayer.setLevel(packet.level);
                    newPlayer.setGold(packet.gold);
                    newPlayer.setExperience(packet.experience);
                    newPlayer.setAttributePoints(packet.attributePoints);

                    otherPlayers.put(packet.playerId, newPlayer);
                    if (playerUI != null) {
                        playerUI.addChatMessage("*** " + packet.username + " entrou no mundo! ***");
                    }
                }
            }
            lastUpdateTime.put(packet.playerId, System.currentTimeMillis());
        });
    }

    public void onPlayerLeft(PlayerLeftPacket packet) {
        Gdx.app.postRunnable(() -> {
            Player removed = otherPlayers.remove(packet.playerId);
            if (removed != null) {
                logger.info("Player removed from world: {}", packet.playerName);
                chatDisplay.append("*** " + packet.playerName + " left the world ***\n");
                if (playerUI != null) {
                    playerUI.updateChatHistory(chatDisplay.toString());
                    playerUI.addChatMessage("*** " + packet.playerName + " left the world ***");
                }
            }
        });
    }

    public void onChatMessage(ChatMessage chat) {
        Gdx.app.postRunnable(() -> {
            String formattedMsg = "SISTEMA".equals(chat.senderName)
                    ? String.format("*** %s ***", chat.message)
                    : String.format("[%s] %s: %s",
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(chat.timestamp)),
                    chat.senderName, chat.message);

            chatDisplay.append(formattedMsg).append("\n");

            String[] lines = chatDisplay.toString().split("\n");
            if (lines.length > 100) {
                int firstNewline = chatDisplay.indexOf("\n");
                if (firstNewline > 0) {
                    chatDisplay.delete(0, firstNewline + 1);
                }
            }

            if (playerUI != null) {
                playerUI.updateChatHistory(chatDisplay.toString());
            }
        });
    }

    public void onChunkReceived(Chunk chunk) {
        Gdx.app.postRunnable(() -> {
            String key = chunk.chunkX + ":" + chunk.chunkY;
            loadedChunks.put(key, chunk);
            logger.debug("Chunk [{},{}] received", chunk.chunkX, chunk.chunkY);
        });
    }

    public void onFriendListResponse(FriendListResponse response) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.updateFriendsList(response);
            }
        });
    }

    public void onFriendRequestResponse(FriendRequestPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                String message = getFriendActionMessage(packet);
                if (message != null) {
                    playerUI.addChatMessage(message);
                }
                if (packet.success && ("ACCEPTED".equals(packet.action) || "REMOVED".equals(packet.action) || "SENT".equals(packet.action))) {
                    refreshFriendList();
                }
            }
        });
    }

    public void onPrivateMessage(PrivateMessagePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.addPrivateMessage(packet.fromUsername, packet.message, packet.timestamp);
                playerUI.addChatMessage("[Privado] " + packet.fromUsername + ": " + packet.message);
            }
        });
    }

    public void onPrivateMessageHistory(PrivateMessageHistoryResponse response) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.loadPrivateChatHistory(response.friendId, response.messages);
            }
        });
    }

    public void onItemSpawn(ItemSpawnPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (itemRenderer != null && packet.item != null) {
                itemRenderer.addItem(packet.item);
                localGroundItems.put(packet.item.getInstanceId(), packet.item);

                ItemDefinition def = packet.item.getDefinition();
                if (def != null && playerUI != null) {
                    registerItemTextureForDefinition(def);
                }

                logger.info("Item spawned in world: {} ({}) at ({}, {})",
                        packet.item.getDefinition().getName(),
                        packet.item.getDefinition().getCategory(),
                        packet.item.getX(),
                        packet.item.getY());
            }
        });
    }

    public void onItemDespawn(ItemDespawnPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (itemRenderer != null) {
                itemRenderer.removeItem(packet.instanceId);
                localGroundItems.remove(packet.instanceId);
                logger.debug("Item despawned: {}", packet.instanceId);
            }
        });
    }

    public void onInventoryUpdate(InventoryUpdatePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (currentPlayer != null && packet.inventory != null) {
                currentPlayer.setInventory(packet.inventory);

                if (playerUI != null) {
                    playerUI.updateInventory(packet.inventory, currentPlayer.getGold());
                }
                logger.info("Inventory updated for {}: {} slots",
                        currentPlayer.getUsername(), packet.inventory.getSlots().size());
            }
        });
    }

    public void onPickupResult(PickupResultPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                if (packet.success) {
                    playerUI.addChatMessage("Você pegou: " + packet.itemName + " x" + packet.quantity);
                } else {
                    playerUI.addChatMessage("Inventário cheio! Não foi possível pegar o item.");
                }
            }
        });
    }

    public void onProjectileState(ProjectileStatePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (projectileAnimRenderer != null) {
                projectileAnimRenderer.onProjectileState(packet);
            }
        });
    }

    public void onAnimationSync(AnimationSyncPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (projectileAnimRenderer != null) {
                logger.info("Received AnimationSync with {} animations",
                        packet.projectileAnimations != null ? packet.projectileAnimations.size() : 0);
                projectileAnimRenderer.onAnimationSync(packet);
            }
        });
    }

    private String getFriendActionMessage(FriendRequestPacket packet) {
        switch (packet.action) {
            case "SENT": return "*** Friend request sent to " + packet.targetUsername + " ***";
            case "ACCEPTED": return "*** " + packet.fromUsername + " accepted your friend request! ***";
            case "REJECTED": return "*** Friend request rejected ***";
            case "REMOVED": return "*** Friend removed ***";
            case "NEW_REQUEST": return "*** " + packet.fromUsername + " sent you a friend request! ***";
            case "ERROR": return "*** Error: " + packet.message + " ***";
            default: return null;
        }
    }

    private void refreshFriendList() {
        if (currentPlayer != null) {
            FriendRequestPacket packet = new FriendRequestPacket("LIST", "");
            game.getNetworkClient().sendPacket(packet);
        }
    }

    private Set<String> collectSpritesheetPaths(MapJSON mapJson) {
        Set<String> paths = new HashSet<>();
        for (Map.Entry<String, MapJSON.ChunkData> entry : mapJson.getChunks().entrySet()) {
            MapJSON.ChunkData jsonChunk = entry.getValue();
            for (int layer = 0; layer < 3; layer++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int y = 0; y < CHUNK_SIZE; y++) {
                        MapJSON.TileData tileData = jsonChunk.getTile(layer, x, y);
                        if (tileData != null && !tileData.isEmpty()) {
                            String path = tileData.getSpritesheetPath();
                            if (path != null && !path.isEmpty()) {
                                paths.add(path);
                            }
                        }
                    }
                }
            }
        }
        return paths;
    }

    private void loadRequiredSpritesheets(Set<String> requiredPaths, MapJSON mapJson) {
        logger.info("Loading required spritesheets: {}", requiredPaths);

        for (String path : requiredPaths) {
            FileHandle file = Gdx.files.internal(path);
            if (!file.exists()) {
                file = Gdx.files.internal("assets/" + path);
            }
            if (!file.exists()) {
                file = Gdx.files.internal("client/assets/" + path);
            }

            if (file.exists()) {
                try {
                    Texture texture = new Texture(file);
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    spritesheets.put(path, texture);

                    int cols = texture.getWidth() / TILE_SIZE;
                    int rows = texture.getHeight() / TILE_SIZE;
                    TextureRegion[][] regions = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
                    spritesheetRegions.put(path, regions);
                } catch (Exception e) {
                    logger.error("Failed to load spritesheet: {}", path, e);
                }
            } else {
                logger.error("Required spritesheet not found: {}", path);
            }
        }

        spritesheetsLoaded = true;
        if (pendingMap != null) {
            loadMapIntoWorld(pendingMap);
            pendingMap = null;
        }
    }

    public void loadMapIntoWorld(MapJSON map) {
        loadedChunks.clear();
        logger.info("Loading map with {} chunks", map.getChunks().size());

        for (Map.Entry<String, MapJSON.ChunkData> entry : map.getChunks().entrySet()) {
            String[] coords = entry.getKey().split(":");
            int chunkX = Integer.parseInt(coords[0]);
            int chunkY = Integer.parseInt(coords[1]);

            MapJSON.ChunkData jsonChunk = entry.getValue();
            Chunk chunk = new Chunk(chunkX, chunkY);

            for (int layer = 0; layer < 3; layer++) {
                for (int x = 0; x < CHUNK_SIZE; x++) {
                    for (int y = 0; y < CHUNK_SIZE; y++) {
                        MapJSON.TileData tileData = jsonChunk.getTile(layer, x, y);
                        if (tileData != null && !tileData.isEmpty()) {
                            String path = tileData.getSpritesheetPath();
                            if (path == null || path.isEmpty()) continue;
                            int tileId = tileData.getTileId();
                            String tag = tileData.getTag();
                            if (tileId < 0) continue;
                            boolean isSolidTile = "solid".equals(tag);
                            chunk.setTile(x, y, path, tileId, isSolidTile, tag);
                        }
                    }
                }
            }
            loadedChunks.put(entry.getKey(), chunk);
        }
        logger.info("Map loaded into world");
    }

    // ==================== SETUP DO INPUT MANAGER ====================

    private void setupPlayerInput() {
        playerInputManager = new PlayerInputManager();

        playerInputManager.setOnDash(() -> {
            if (currentPlayer != null && currentPlayer.canDash()) {
                if (currentPlayer.executeDash()) {
                    isDashing = true;
                    dashTimer = Player.getDashDuration();
                    dashStartPos.set(currentPlayer.getX(), currentPlayer.getY());
                    dashDirection.set(playerInputManager.getDashDirection());

                    if (playerUI != null) {
                        float staminaPercent = (float) currentPlayer.getCurrentStamina() / currentPlayer.getMaxStamina() * 100;
                        playerUI.setStamina(staminaPercent);
                    }
                }
            }
        });

        playerInputManager.setOnSprintStart(() -> {});
        playerInputManager.setOnSprintEnd(() -> {});

        logger.info("PlayerInputManager initialized");
    }

    // ==================== SISTEMA DE PICKUP ====================

    private void checkItemPickup() {
        if (currentPlayer == null) return;

        long now = System.currentTimeMillis();
        if (now - lastPickupCheck < PICKUP_CHECK_INTERVAL_MS) return;
        lastPickupCheck = now;

        for (GroundItem item : localGroundItems.values()) {
            float dx = currentPlayer.getX() - item.getX();
            float dy = currentPlayer.getY() - item.getY();
            float distSq = dx * dx + dy * dy;

            if (distSq < PICKUP_RANGE * PICKUP_RANGE) {
                logger.info("Picking up item: {} at distance {}",
                        item.getDefinition().getName(), Math.sqrt(distSq));

                PickupItemPacket packet = new PickupItemPacket(item.getInstanceId(), currentPlayer.getId());
                game.getNetworkClient().sendPacket(packet);
                break;
            }
        }
    }

    // ==================== SISTEMA DE COMBATE ====================
    private float getRemainingCooldown() {
        if (currentPlayer == null) return 0;
        long now = System.currentTimeMillis();
        long elapsed = now - currentPlayer.getLastAttackTime();
        float cooldownSecs = currentPlayer.getCurrentAttackCooldown();
        long cooldownMillis = (long)(cooldownSecs * 1000);
        float remaining = (cooldownMillis - elapsed) / 1000f;
        return Math.max(0, remaining);
    }

    private void performAttack() {
        if (currentPlayer == null) return;

        // Rate limit para evitar spam de pacotes
        long now = System.currentTimeMillis();
        if (now - lastAttackTime < 100) {
            return;
        }
        lastAttackTime = now;

        // Verificar se clicou no chat
        if (playerUI != null && playerUI.isPointOverChat(Gdx.input.getX(), Gdx.input.getY())) {
            return;
        }

        // Obter posição do mouse no mundo
        Vector3 mousePos3D = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
        gameCamera.getCamera().unproject(mousePos3D);
        float mouseWorldX = mousePos3D.x;
        float mouseWorldY = mousePos3D.y;

        // Obter arma equipada
        String weaponId = currentPlayer.getInventory().getEquipped().get("weapon");
        AttackDefinition attackDef;

        if (weaponId != null && !weaponId.isEmpty()) {
            ItemDefinition weaponDef = getItemDefinition(weaponId);
            if (weaponDef != null) {
                attackDef = createAttackDefinitionFromWeapon(weaponDef);
            } else {
                attackDef = AttackDefinition.createMeleeSword();
            }
        } else {
            attackDef = AttackDefinition.createMeleeSword();
            attackDef.setName("Soco");
            attackDef.setDamageMultiplier(0.5f);
        }


        // Enviar para o servidor
        AttackInfo attackInfo = new AttackInfo(
                currentPlayer.getId(),
                currentPlayer.getUsername(),
                currentPlayer.getX(),
                currentPlayer.getY(),
                attackDef.getId(),
                mouseWorldX,
                mouseWorldY
        );
        game.getNetworkClient().sendPacket(attackInfo);

        // Feedback visual imediato
        if (playerUI != null) {
            playerUI.addChatMessage("⚔️ " + attackDef.getName() + "!");
        }
    }

    // Método auxiliar para criar AttackDefinition a partir do ItemDefinition
    private AttackDefinition createAttackDefinitionFromWeapon(ItemDefinition weaponDef) {
        AttackDefinition def = new AttackDefinition();

        def.setId(weaponDef.getAttackId());
        def.setName(weaponDef.getName());
        def.setAttackAnimation(weaponDef.getAttackAnimation());
        def.setRanged(weaponDef.isRanged());

        if (weaponDef.isRanged()) {
            // Ataque à distância
            def.setHitboxType(AttackHitboxType.CIRCLE);
            def.setRange(weaponDef.getProjectileRange());
            def.setRadius(24f);
            def.setDamageMultiplier(1.0f);
            def.setCooldownSeconds(1.0f / weaponDef.getAttackSpeed());
            def.setStaminaCost(10f);
            def.setMaxTargets(1);
            def.setKnockbackPower(15f);
            def.setProjectileId(weaponDef.getProjectileId());
            def.setProjectileSpeed(weaponDef.getProjectileSpeed());
        } else {
            // Ataque corpo a corpo
            def.setHitboxType(AttackHitboxType.RECTANGLE);
            def.setRange(64f);
            def.setWidth(48f);
            def.setHeight(32f);
            def.setDamageMultiplier(weaponDef.getDamage() / 10.0f);
            def.setCooldownSeconds(1.0f / weaponDef.getAttackSpeed());
            def.setStaminaCost(5f);
            def.setMaxTargets(3);
            def.setKnockbackPower(30f);
        }

        return def;
    }

    private ItemDefinition getItemDefinition(String itemId) {
        // Agora usa o cache vindo do servidor
        ItemDefinition def = itemDefinitionCache.get(itemId);
        if (def != null) {
            return def;
        }
        switch (itemId) {
            case "simple_bow":
                def.setName("Arco Simples");
                def.setAttackId("ranged_bow");
                def.setAttackAnimation("bow_shoot");
                def.setRanged(true);
                def.setProjectileId("arrow");
                def.setProjectileSpeed(600f);
                def.setProjectileRange(400f);
                def.setAttackSpeed(0.8f);
                def.setDamage(8);
                break;
            case "long_bow":
                def.setName("Arco Longo");
                def.setAttackId("ranged_bow");
                def.setAttackAnimation("bow_shoot");
                def.setRanged(true);
                def.setProjectileId("arrow");
                def.setProjectileSpeed(900f);
                def.setProjectileRange(550f);
                def.setAttackSpeed(0.6f);
                def.setDamage(15);
                break;
            case "quick_bow":
                def.setName("Arco Rápido");
                def.setAttackId("ranged_bow");
                def.setAttackAnimation("bow_shoot");
                def.setRanged(true);
                def.setProjectileId("arrow");
                def.setProjectileSpeed(450f);
                def.setProjectileRange(300f);
                def.setAttackSpeed(1.3f);
                def.setDamage(6);
                break;
            case "dagger":
                def.setName("Adaga");
                def.setAttackId("melee_dagger");
                def.setAttackAnimation("dagger_stab");
                def.setRanged(false);
                def.setAttackSpeed(1.5f);
                def.setDamage(6);
                break;
            case "simple_axe":
                def.setName("Machado Simples");
                def.setAttackId("melee_axe");
                def.setAttackAnimation("sword_slash");
                def.setRanged(false);
                def.setAttackSpeed(0.7f);
                def.setDamage(14);
                break;
            case "iron_sword":
                def.setName("Espada de Ferro");
                def.setAttackId("melee_sword");
                def.setAttackAnimation("sword_slash");
                def.setRanged(false);
                def.setAttackSpeed(0.8f);
                def.setDamage(18);
                break;
            default:
                def.setName("Espada Simples");
                def.setAttackId("melee_sword");
                def.setAttackAnimation("sword_slash");
                def.setRanged(false);
                def.setAttackSpeed(1.0f);
                def.setDamage(10);
        }

        itemDefinitionCache.put(itemId, def);
        return def;
    }

    public void onItemDefinitionSync(ItemDefinitionSyncPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (packet.itemDefinitions == null) return;

            itemDefinitionCache.clear();
            itemDefinitionCache.putAll(packet.itemDefinitions);

            logger.info("Cached {} item definitions from server", itemDefinitionCache.size());

            // Registrar texturas no PlayerUI para todos os itens
            if (playerUI != null) {
                for (ItemDefinition def : itemDefinitionCache.values()) {
                    registerItemTextureForDefinition(def);
                }
            }

            // Se já temos inventário, atualizar as texturas
            if (currentPlayer != null && currentPlayer.getInventory() != null) {
                refreshInventoryTextures();
            }
        });
    }

    private void registerItemTextureForDefinition(ItemDefinition def) {
        String spritesheetPath = def.getSpritesheet();
        TextureRegion[][] regions = spritesheetRegions.get(spritesheetPath);
        TextureRegion icon = null;

        if (regions != null && def.getTileY() < regions.length && def.getTileX() < regions[0].length) {
            icon = regions[def.getTileY()][def.getTileX()];
        }

        if (playerUI != null) {
            playerUI.registerItemTexture(def.getId(), icon, def);
        }
    }

    private void refreshInventoryTextures() {
        if (currentPlayer == null || currentPlayer.getInventory() == null) return;

        // Re-registrar todos os itens do inventário
        for (ItemStack stack : currentPlayer.getInventory().getSlots().values()) {
            if (stack != null && !stack.isEmpty()) {
                ItemDefinition def = itemDefinitionCache.get(stack.getItemId());
                if (def != null) {
                    registerItemTextureForDefinition(def);
                }
            }
        }

        // Atualizar UI
        if (playerUI != null) {
            playerUI.updateInventory(currentPlayer.getInventory(), currentPlayer.getGold());
        }
    }
    // ==================== MÉTODOS DE MOVIMENTO ====================

    private void handleInput(float delta) {
        // Verificar se UI está bloqueando input
        if (playerUI != null && (playerUI.isChatFocused() ||
                playerUI.isFriendsWindowVisible() ||
                playerUI.isAttributesVisible() ||
                playerUI.isPrivateChatVisible() ||
                playerUI.isInventoryVisible())) {
            if (playerInputManager != null) {
                playerInputManager.setInputBlocked(true);
            }
            return;
        }

        if (playerInputManager != null) {
            playerInputManager.setInputBlocked(false);
            playerInputManager.update(delta);
        }

        if (currentPlayer == null) return;

        // Atualizar regeneração
        currentPlayer.updateRegeneration(delta);
        currentPlayer.updateAttack(delta);

        if (playerUI != null) {
            playerUI.setStamina(currentPlayer.getStaminaPercentage() * 100);
            playerUI.setHealth(currentPlayer.getHpPercentage() * 100);
            playerUI.setMana(currentPlayer.getManaPercentage() * 100);
        }

        // Detectar clique esquerdo para atacar
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            performAttack();
        }

        if (isDashing) {
            handleDash(delta);
            return;
        }

        if (playerInputManager.isDashJustExecuted()) {
            if (!isDashing && currentPlayer.getDashTimer() > 0) {
                isDashing = true;
                dashTimer = currentPlayer.getDashTimer();
                dashStartPos.set(currentPlayer.getX(), currentPlayer.getY());
                dashDirection.set(playerInputManager.getDashDirection());
            }
            return;
        }

        Vector2 dir = playerInputManager.getMovementDirection();
        boolean sprinting = playerInputManager.isSprinting();

        float playerSpeedMultiplier = 1.0f;
        boolean moved = false;

        if (dir.x != 0 || dir.y != 0) {
            moved = true;
            float baseSpeed = currentPlayer.getMovementSpeed();
            float terrainSpeed = getCurrentTerrainSpeed();

            if (sprinting && currentPlayer.getCurrentStamina() > 0) {
                playerSpeedMultiplier = Player.getSprintMultiplier();
                if (!currentPlayer.consumeStaminaForSprint(delta)) {
                    sprinting = false;
                    playerSpeedMultiplier = 1.0f;
                }
                if (playerUI != null) {
                    playerUI.setStamina(currentPlayer.getStaminaPercentage() * 100);
                }
            }

            float speed = baseSpeed * terrainSpeed * playerSpeedMultiplier * delta;
            String newDirection = getDirectionFromVector(dir);

            float newX = currentPlayer.getX() + dir.x * speed;
            float newY = currentPlayer.getY() + dir.y * speed;

            if (!isColliding(newX, currentPlayer.getY())) {
                currentPlayer.setX(newX);
            }
            if (!isColliding(currentPlayer.getX(), newY)) {
                currentPlayer.setY(newY);
            }

            currentPlayer.setDirection(newDirection);
        }

        long now = System.currentTimeMillis();

        // Enviar a CADA MOVIMENTO (rate limited)
        if (moved && (now - lastMovementSendTime) >= MOVEMENT_SEND_INTERVAL_MS) {
            sendPlayerState();
            lastMovementSendTime = now;
        }

        // Sincronização periódica de status (a cada 10 segundos)
        if ((now - lastStatusSyncTime) >= STATUS_SYNC_INTERVAL_MS) {
            sendPlayerState();
            lastStatusSyncTime = now;
        }

        if (playerUI != null) {
            playerUI.setSpeedMultiplier(playerSpeedMultiplier);
            playerUI.update(currentPlayer, getCurrentTerrainSpeed());
        }
    }

    private void sendPlayerState() {
        if (currentPlayer == null || game.getNetworkClient() == null) return;

        PlayerStatePacket packet = new PlayerStatePacket(currentPlayer);
        game.getNetworkClient().sendPlayerState(packet);
    }

    private void handleDash(float delta) {
        dashTimer -= delta;
        currentPlayer.setDashTimer(dashTimer);

        if (dashTimer <= 0) {
            isDashing = false;
            currentPlayer.setDashing(false);
            currentPlayer.setDashTimer(0);
            return;
        }

        float progress = 1.0f - (dashTimer / Player.getDashDuration());
        float dashDistance = Player.getDashDistance();

        float newX = dashStartPos.x + dashDirection.x * dashDistance * progress;
        float newY = dashStartPos.y + dashDirection.y * dashDistance * progress;

        if (!isColliding(newX, newY)) {
            currentPlayer.setX(newX);
            currentPlayer.setY(newY);
        } else {
            isDashing = false;
            currentPlayer.setDashing(false);
            currentPlayer.setDashTimer(0);
        }
    }

    private String getDirectionFromVector(Vector2 dir) {
        if (Math.abs(dir.x) > Math.abs(dir.y)) {
            return dir.x > 0 ? "RIGHT" : "LEFT";
        }
        return dir.y > 0 ? "UP" : "DOWN";
    }

    // ==================== RENDERIZAÇÃO ====================

    private void renderChunks() {
        if (currentPlayer == null) return;
        if (spritesheets.isEmpty()) return;

        int centerChunkX = (int) Math.floor(currentPlayer.getX() / (CHUNK_SIZE * WORLD_TILE_SIZE));
        int centerChunkY = (int) Math.floor(currentPlayer.getY() / (CHUNK_SIZE * WORLD_TILE_SIZE));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int chunkX = centerChunkX + dx;
                int chunkY = centerChunkY + dy;
                Chunk chunk = loadedChunks.get(chunkX + ":" + chunkY);
                if (chunk != null) {
                    renderChunk(chunk, chunkX, chunkY);
                }
            }
        }
    }

    private void renderChunk(Chunk chunk, int chunkX, int chunkY) {
        float offsetX = chunkX * CHUNK_SIZE * WORLD_TILE_SIZE;
        float offsetY = chunkY * CHUNK_SIZE * WORLD_TILE_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                WorldTile tile = chunk.getTile(x, y);
                if (tile != null && tile.tileId >= 0 && tile.spritesheetPath != null && !tile.spritesheetPath.isEmpty()) {
                    TextureRegion region = getTileRegion(tile.spritesheetPath, tile.tileId);
                    if (region != null) {
                        batch.draw(region,
                                offsetX + x * WORLD_TILE_SIZE,
                                offsetY + y * WORLD_TILE_SIZE,
                                WORLD_TILE_SIZE, WORLD_TILE_SIZE);
                    }
                }
            }
        }
    }

    private TextureRegion getTileRegion(String path, int tileId) {
        TextureRegion[][] regions = spritesheetRegions.get(path);
        if (regions == null) return null;

        int cols = regions[0].length;
        int row = tileId / cols;
        int col = tileId % cols;

        if (row >= 0 && row < regions.length && col >= 0 && col < regions[0].length) {
            return regions[row][col];
        }
        return null;
    }

    private boolean isColliding(float x, float y) {
        float halfSize = PLAYER_SIZE / 2;

        float[][] corners = {
                {x - halfSize, y - halfSize},
                {x + halfSize, y - halfSize},
                {x - halfSize, y + halfSize},
                {x + halfSize, y + halfSize}
        };

        for (float[] corner : corners) {
            int chunkX = (int) Math.floor(corner[0] / (CHUNK_SIZE * WORLD_TILE_SIZE));
            int chunkY = (int) Math.floor(corner[1] / (CHUNK_SIZE * WORLD_TILE_SIZE));
            int localX = (int) (corner[0] % (CHUNK_SIZE * WORLD_TILE_SIZE)) / WORLD_TILE_SIZE;
            int localY = (int) (corner[1] % (CHUNK_SIZE * WORLD_TILE_SIZE)) / WORLD_TILE_SIZE;

            if (localX < 0) localX += CHUNK_SIZE;
            if (localY < 0) localY += CHUNK_SIZE;

            if (localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE) {
                String key = chunkX + ":" + chunkY;
                Chunk chunk = loadedChunks.get(key);

                if (chunk != null) {
                    WorldTile tile = chunk.getTile(localX, localY);
                    if (tile == null || tile.isEmpty() || tile.isSolid()) {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }
        return false;
    }

    private float getCurrentTerrainSpeed() {
        if (currentPlayer == null) return 1.0f;

        float playerX = currentPlayer.getX();
        float playerY = currentPlayer.getY();
        float halfSize = PLAYER_SIZE / 2;

        float checkX = playerX;
        float checkY = playerY - halfSize + 8;

        int chunkSizeWorld = CHUNK_SIZE * WORLD_TILE_SIZE;
        int chunkX = (int) Math.floor(checkX / chunkSizeWorld);
        int chunkY = (int) Math.floor(checkY / chunkSizeWorld);
        int localX = (int) (checkX % chunkSizeWorld) / WORLD_TILE_SIZE;
        int localY = (int) (checkY % chunkSizeWorld) / WORLD_TILE_SIZE;

        if (localX < 0) localX += CHUNK_SIZE;
        if (localY < 0) localY += CHUNK_SIZE;

        if (localX >= 0 && localX < CHUNK_SIZE && localY >= 0 && localY < CHUNK_SIZE) {
            String key = chunkX + ":" + chunkY;
            Chunk chunk = loadedChunks.get(key);
            if (chunk != null) {
                WorldTile tile = chunk.getTile(localX, localY);
                if (tile != null && !tile.isEmpty()) {
                    return tile.getWalkSpeed();
                }
            }
        }

        return 1.0f;
    }

    private void renderHealthBars() {
        if (otherPlayers.isEmpty()) return;

        // NÃO chamar shapeRenderer.begin() aqui - ela já está aberta no método render()

        for (Player player : otherPlayers.values()) {
            if (player == null) continue;

            // Calcular posicao do player
            float renderX = player.getX();
            float renderY = player.getY();

            // Interpolacao suave
            Player interpolated = interpolatedPlayers.get(player.getId());
            Long lastUpdate = lastUpdateTime.get(player.getId());
            long now = System.currentTimeMillis();

            if (interpolated != null && lastUpdate != null) {
                long elapsed = Math.min(now - lastUpdate, 100);
                float alpha = Math.min(1.0f, elapsed / 50.0f);
                renderX = interpolated.getX() + (player.getX() - interpolated.getX()) * alpha;
                renderY = interpolated.getY() + (player.getY() - interpolated.getY()) * alpha;

                if (alpha >= 0.99f) {
                    interpolatedPlayers.remove(player.getId());
                    lastUpdateTime.remove(player.getId());
                }
            }

            // Dimensoes da barra
            float barWidth = 70;
            float barHeight = 8;
            float barX = renderX - barWidth / 2;
            float barY = renderY + PLAYER_SIZE / 2 + 10;

            // Calcular percentual de vida
            int currentHp = player.getCurrentHp();
            int maxHp = player.getMaxHp();
            float healthPercent = (maxHp > 0) ? (float) currentHp / maxHp : 0f;

            // Fundo (cinza escuro)
            shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
            shapeRenderer.rect(barX, barY, barWidth, barHeight);

            // Preenchimento (cor baseada na vida)
            float fillWidth = barWidth * healthPercent;
            if (fillWidth > 0) {
                if (healthPercent > 0.6f) {
                    shapeRenderer.setColor(0.2f, 0.8f, 0.2f, 0.9f);
                } else if (healthPercent > 0.3f) {
                    shapeRenderer.setColor(0.9f, 0.7f, 0.2f, 0.9f);
                } else {
                    shapeRenderer.setColor(0.9f, 0.2f, 0.2f, 0.9f);
                }
                shapeRenderer.rect(barX + 1, barY + 1, Math.max(0, fillWidth - 2), barHeight - 2);
            }
        }
    }

    /**
     * Renderiza os players (quadrados)
     */
    private void renderPlayers() {
        long now = System.currentTimeMillis();

        shapeRenderer.setProjectionMatrix(gameCamera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Player player : otherPlayers.values()) {
            float renderX = player.getX();
            float renderY = player.getY();

            Player interpolated = interpolatedPlayers.get(player.getId());
            Long lastUpdate = lastUpdateTime.get(player.getId());

            if (interpolated != null && lastUpdate != null) {
                long elapsed = Math.min(now - lastUpdate, 100);
                float alpha = Math.min(1.0f, elapsed / 50.0f);
                renderX = interpolated.getX() + (player.getX() - interpolated.getX()) * alpha;
                renderY = interpolated.getY() + (player.getY() - interpolated.getY()) * alpha;

                if (alpha >= 0.99f) {
                    interpolatedPlayers.remove(player.getId());
                    lastUpdateTime.remove(player.getId());
                }
            }

            float x = renderX - PLAYER_SIZE/2;
            float y = renderY - PLAYER_SIZE/2;

            // Sombra
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);

            // Corpo
            if (player.isAttacking()) {
                shapeRenderer.setColor(0.9f, 0.3f, 0.2f, 1);
            } else {
                shapeRenderer.setColor(0.5f, 0.7f, 0.3f, 1);
            }
            shapeRenderer.rect(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }

        if (currentPlayer != null) {
            float x = currentPlayer.getX() - PLAYER_SIZE/2;
            float y = currentPlayer.getY() - PLAYER_SIZE/2;

            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);

            if (currentPlayer.isAttacking()) {
                shapeRenderer.setColor(0.95f, 0.4f, 0.3f, 1);
            } else {
                shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 1);
            }
            shapeRenderer.rect(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }

        shapeRenderer.end();
    }

    private void renderFloatingNames() {
        long now = System.currentTimeMillis();

        for (Player player : otherPlayers.values()) {
            float renderX = player.getX();
            float renderY = player.getY();

            Player interpolated = interpolatedPlayers.get(player.getId());
            Long lastUpdate = lastUpdateTime.get(player.getId());

            if (interpolated != null && lastUpdate != null) {
                long elapsed = Math.min(now - lastUpdate, 100);
                float alpha = Math.min(1.0f, elapsed / 50.0f);
                renderX = interpolated.getX() + (player.getX() - interpolated.getX()) * alpha;
                renderY = interpolated.getY() + (player.getY() - interpolated.getY()) * alpha;
            }

            // Nome do jogador (acima da barra de vida)
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, player.getUsername(),
                    renderX - (player.getUsername().length() * 4),
                    renderY + PLAYER_SIZE/2 + 35);
        }

        if (currentPlayer != null) {
            font.setColor(1f, 0.9f, 0.2f, 1f);
            font.draw(batch, currentPlayer.getUsername(),
                    currentPlayer.getX() - (currentPlayer.getUsername().length() * 4),
                    currentPlayer.getY() + PLAYER_SIZE/2 + 35);
        }
    }

    private void renderAttackCooldown() {
        if (currentPlayer == null) return;

        // Verificar se está em cooldown
        if (!currentPlayer.canAttack()) {
            long now = System.currentTimeMillis();
            long elapsed = now - currentPlayer.getLastAttackTime();
            float cooldownSecs = currentPlayer.getCurrentAttackCooldown();
            long cooldownMillis = (long)(cooldownSecs * 1000);
            float percent = 1.0f - (float) elapsed / cooldownMillis;

            if (percent > 0 && percent < 1.0f) {
                // Posição fixa no canto superior direito da TELA
                float x = Gdx.graphics.getWidth() - 80;
                float y = Gdx.graphics.getHeight() - 80;

                OrthographicCamera uiCam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                uiCam.setToOrtho(false);
                shapeRenderer.setProjectionMatrix(uiCam.combined);

                shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                // Fundo escuro
                shapeRenderer.setColor(0.2f, 0.2f, 0.2f, 0.8f);
                shapeRenderer.rect(x, y, 60, 60);
                // Círculo de cooldown
                shapeRenderer.setColor(0.8f, 0.2f, 0.2f, 0.8f);
                shapeRenderer.rect(x, y, 60, 60 * percent);
                shapeRenderer.end();

                // Texto do tempo restante
                float remaining = (cooldownMillis - elapsed) / 1000f;
                font.setColor(Color.WHITE);
                batch.begin();
                font.draw(batch, String.format("%.1f", remaining), x + 20, y + 35);
                batch.end();
            }
        }
    }

    private void createPlayerUI() {
        playerUI = new PlayerUI();
        playerUI.setGame(game);

        playerUI.setChatInputProcessor(chatInput -> {
            String message = chatInput.trim();
            if (!message.isEmpty() && currentPlayer != null) {
                game.getNetworkClient().sendChat(currentPlayer.getId(), currentPlayer.getUsername(), message);
            }
        });

        // Friend system callbacks
        playerUI.setSendFriendRequestCallback(username -> {
            if (currentPlayer != null) {
                FriendRequestPacket packet = new FriendRequestPacket("SEND", username);
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setAcceptFriendRequestCallback(requestId -> {
            if (currentPlayer != null) {
                FriendRequestPacket packet = new FriendRequestPacket("ACCEPT", "");
                packet.requestId = requestId;
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setRejectFriendRequestCallback(requestId -> {
            if (currentPlayer != null) {
                FriendRequestPacket packet = new FriendRequestPacket("REJECT", "");
                packet.requestId = requestId;
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setRemoveFriendCallback(username -> {
            if (currentPlayer != null) {
                FriendRequestPacket packet = new FriendRequestPacket("REMOVE", username);
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setSendPrivateMessageCallback((friendId, message) -> {
            if (currentPlayer != null) {
                PrivateMessagePacket packet = new PrivateMessagePacket(
                        currentPlayer.getId(), currentPlayer.getUsername(),
                        friendId, "", message
                );
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setRefreshFriendsCallback(() -> {
            if (currentPlayer != null) {
                FriendRequestPacket packet = new FriendRequestPacket("LIST", "");
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setOnLoadPrivateChatHistory(friendId -> {
            if (currentPlayer != null) {
                logger.info("Requesting private message history for friend: {}", friendId);
                game.getNetworkClient().requestPrivateMessageHistory(friendId, 100);
            }
        });

        // Inventory system callbacks
        playerUI.setOnMoveItemCallback((fromSlot, toSlot) -> {
            if (currentPlayer != null) {
                InventoryUpdatePacket packet = new InventoryUpdatePacket();
                packet.action = "MOVE_ITEM";
                packet.slot = fromSlot;
                packet.targetSlot = toSlot;
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setOnEquipItemCallback((slot, equipSlot) -> {
            if (currentPlayer != null) {
                InventoryUpdatePacket packet = new InventoryUpdatePacket();
                packet.action = "EQUIP";
                packet.slot = slot;
                packet.equipSlot = equipSlot;
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setOnUnequipItemCallback(slotIndex -> {
            if (currentPlayer != null) {
                String equipSlot;
                switch (slotIndex) {
                    case 0: equipSlot = "weapon"; break;
                    case 1: equipSlot = "helmet"; break;
                    case 2: equipSlot = "chest"; break;
                    case 3: equipSlot = "legs"; break;
                    case 4: equipSlot = "boots"; break;
                    default: equipSlot = "weapon"; break;
                }
                InventoryUpdatePacket packet = new InventoryUpdatePacket();
                packet.action = "UNEQUIP";
                packet.equipSlot = equipSlot;
                game.getNetworkClient().sendPacket(packet);
            }
        });

        playerUI.setOnDropItemCallback(action -> {
            if (currentPlayer != null) {
                ItemStack stack = currentPlayer.getInventory().getSlot(action.slot);
                if (stack != null && !stack.isEmpty()) {
                    DropItemPacket packet = new DropItemPacket(action.slot, action.quantity, currentPlayer.getId());
                    game.getNetworkClient().sendPacket(packet);
                    playerUI.addChatMessage("Item dropped: " + stack.getItemId() + " x" + action.quantity);
                }
            }
        });

        playerUI.updateChatHistory(chatDisplay.toString());
        playerUI.addChatMessage("*** Welcome to Sandbox Experiment! ***");
        playerUI.addChatMessage("*** Use WASD to move ***");
        playerUI.addChatMessage("*** SHIFT to sprint | SPACE to dash ***");
        playerUI.addChatMessage("*** LEFT CLICK to attack! ***");
        playerUI.addChatMessage("*** Press ENTER to chat | H to hide chat | C for attributes | I for inventory ***");
        playerUI.addChatMessage("*** Click FRIENDS button to manage friends ***");

        if (adminMode) {
            playerUI.addChatMessage("*** ADMIN MODE: Press F12 for Map Editor ***");
        }

        playerUI.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private InputProcessor createInputProcessor() {
        if (playerInputManager == null) {
            setupPlayerInput();
        }

        final PlayerInputManager inputManager = playerInputManager;

        return new InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                if (inputManager != null && inputManager.keyDown(keycode)) {
                    return true;
                }

                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        playerUI.getStage().keyDown(keycode);
                    }
                    if (keycode == Input.Keys.ENTER) {
                        playerUI.sendPrivateChatMessage();
                        return true;
                    }
                    if (keycode == Input.Keys.ESCAPE) {
                        playerUI.closePrivateChat();
                        return true;
                    }
                    return true;
                }

                if (playerUI != null && playerUI.isFriendsWindowVisible()) {
                    if (playerUI.getStage() != null) {
                        playerUI.getStage().keyDown(keycode);
                    }
                    if (keycode == Input.Keys.ESCAPE) {
                        playerUI.toggleFriendsWindow();
                    }
                    return true;
                }

                if (playerUI != null && playerUI.isAttributesVisible()) {
                    if (keycode == Input.Keys.ESCAPE) {
                        playerUI.hideAttributes();
                        return true;
                    }
                    return true;
                }

                if (playerUI != null && playerUI.isInventoryVisible()) {
                    if (keycode == Input.Keys.ESCAPE || keycode == Input.Keys.I) {
                        playerUI.toggleInventory();
                        return true;
                    }
                    return true;
                }

                boolean chatFocused = playerUI != null && playerUI.isChatFocused();

                if (keycode == Input.Keys.F && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleFriendsWindow();
                    }
                    return true;
                }

                if (keycode == Input.Keys.C && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleAttributes();
                    }
                    return true;
                }

                if (keycode == Input.Keys.I && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleInventory();
                    }
                    return true;
                }

                if (keycode == Input.Keys.H && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleChat();
                    }
                    return true;
                }

                if (keycode == Input.Keys.ENTER) {
                    if (playerUI != null) {
                        if (playerUI.isChatFocused()) {
                            playerUI.sendCurrentMessage();
                        } else {
                            playerUI.focusChat();
                        }
                    }
                    return true;
                }

                if (keycode == Input.Keys.ESCAPE) {
                    if (playerUI != null && playerUI.isChatFocused()) {
                        playerUI.unfocusChat();
                        return true;
                    }
                }

                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyDown(keycode);
                }

                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if (inputManager != null && inputManager.keyUp(keycode)) {
                    return true;
                }

                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().keyUp(keycode);
                    }
                    return true;
                }

                boolean chatFocused = playerUI != null && playerUI.isChatFocused();
                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyUp(keycode);
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().keyTyped(character);
                    }
                    return true;
                }

                boolean chatFocused = playerUI != null && playerUI.isChatFocused();
                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyTyped(character);
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().touchDown(screenX, screenY, pointer, button);
                    }
                    return true;
                }

                if (playerUI != null && playerUI.getStage() != null) {
                    boolean handled = playerUI.getStage().touchDown(screenX, screenY, pointer, button);
                    if (handled) {
                        return true;
                    }
                }

                if (playerUI != null && playerUI.isChatFocused()) {
                    if (!playerUI.isPointOverChat(screenX, screenY)) {
                        playerUI.unfocusChat();
                    }
                    return true;
                }

                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().touchUp(screenX, screenY, pointer, button);
                    }
                    return true;
                }

                if (playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().touchUp(screenX, screenY, pointer, button);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().touchDragged(screenX, screenY, pointer);
                    }
                    return true;
                }

                if (playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().touchDragged(screenX, screenY, pointer);
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().mouseMoved(screenX, screenY);
                    }
                    return true;
                }

                if (playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().mouseMoved(screenX, screenY);
                }
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                if (playerUI != null && playerUI.isPrivateChatVisible()) {
                    if (playerUI.getStage() != null) {
                        return playerUI.getStage().scrolled(amountX, amountY);
                    }
                    return true;
                }

                if (playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().scrolled(amountX, amountY);
                }
                return false;
            }

            @Override
            public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                return false;
            }
        };
    }

    // ==================== MÉTODOS DA SCREEN ====================

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        logger.info("=== SET CURRENT PLAYER ===");
        logger.info("Current player: {} at ({}, {})", player.getUsername(), player.getX(), player.getY());
        logger.info("HP from Player object: {}/{}", player.getCurrentHp(), player.getMaxHp());
        logger.info("==========================");

        currentPlayer.setOnStatusChanged(() -> {
            // Enviar atualizacao para o servidor quando qualquer status mudar
            Gdx.app.postRunnable(() -> {
                sendPlayerState();
                logger.debug("Status changed for {}, sending update", currentPlayer.getUsername());
            });
        });

        chatDisplay.append("*** Welcome to Sandbox Experiment! ***\n");
        chatDisplay.append("*** Use WASD to move ***\n");
        chatDisplay.append("*** SHIFT to sprint | SPACE to dash ***\n");
        chatDisplay.append("*** LEFT CLICK to attack! ***\n");
        chatDisplay.append("*** Press ENTER to chat | H to hide chat | C for attributes | I for inventory ***\n\n");
        if (adminMode) {
            chatDisplay.append("*** ADMIN MODE: Press F12 for Map Editor ***\n");
        }

        if (playerUI != null) {
            playerUI.update(currentPlayer, 1.0f);
            playerUI.updateChatHistory(chatDisplay.toString());

            playerUI.setHealth(player.getCurrentHp(), player.getMaxHp());
            playerUI.setMana(player.getCurrentMana(), player.getMaxMana());
            playerUI.setStamina(player.getCurrentStamina(), player.getMaxStamina());
            playerUI.setGold(player.getGold());
            playerUI.updateInventory(player.getInventory(), player.getGold());
        }

        if (gameCamera != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.resetPosition();
        }
    }

    @Override
    public void show() {
        logger.info("GameWorldRenderer show()");
        if (!initialized) {
            init();
        }
    }

    @Override
    public void render(float delta) {
        if (!initialized) return;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (adminMode && Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            game.setScreen(new MapEditorScreen(game));
            return;
        }

        handleInput(delta);
        checkItemPickup();

        if (attackHitboxRenderer != null) {
            attackHitboxRenderer.update(delta);
        }

        if (attackEffectManager != null) {
            attackEffectManager.update(delta);
        }

        if (projectileAnimRenderer != null) {
            projectileAnimRenderer.update(delta);
        }

        if (currentPlayer != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.update(delta);
        }

        // ==================== RENDERIZAÇÃO COM BATCH (TEXTURAS - CAMADA DE FUNDO) ====================
        batch.setProjectionMatrix(gameCamera.getCamera().combined);
        batch.begin();

        renderChunks();  // Chão do mapa

        if (itemRenderer != null) {
            itemRenderer.render(batch, gameCamera);  // Itens no chão
        }

        renderFloatingNames();  // Nomes dos players

        if (attackHitboxRenderer != null) {
            attackHitboxRenderer.renderInfo(font, batch, gameCamera);  // Texto debug da hitbox
        }

        batch.end();
        // ==================== FIM DO BATCH (CAMADA DE FUNDO) ====================

        // ==================== RENDERIZAÇÃO COM SHAPERENDERER (PLAYERS E HITBOX) ====================
        shapeRenderer.setProjectionMatrix(gameCamera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Desenhar barras de vida dos players
        renderHealthBars();

        // Desenhar players (quadrados)
        for (Player player : otherPlayers.values()) {
            float renderX = player.getX();
            float renderY = player.getY();

            Player interpolated = interpolatedPlayers.get(player.getId());
            Long lastUpdate = lastUpdateTime.get(player.getId());

            if (interpolated != null && lastUpdate != null) {
                long elapsed = Math.min(System.currentTimeMillis() - lastUpdate, 100);
                float alpha = Math.min(1.0f, elapsed / 50.0f);
                renderX = interpolated.getX() + (player.getX() - interpolated.getX()) * alpha;
                renderY = interpolated.getY() + (player.getY() - interpolated.getY()) * alpha;

                if (alpha >= 0.99f) {
                    interpolatedPlayers.remove(player.getId());
                    lastUpdateTime.remove(player.getId());
                }
            }

            float px = renderX - PLAYER_SIZE/2;
            float py = renderY - PLAYER_SIZE/2;

            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(px - 2, py - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);

            if (player.isAttacking()) {
                shapeRenderer.setColor(0.9f, 0.3f, 0.2f, 1);
            } else {
                shapeRenderer.setColor(0.5f, 0.7f, 0.3f, 1);
            }
            shapeRenderer.rect(px, py, PLAYER_SIZE, PLAYER_SIZE);
        }

        // Desenhar próprio player
        if (currentPlayer != null) {
            float px = currentPlayer.getX() - PLAYER_SIZE/2;
            float py = currentPlayer.getY() - PLAYER_SIZE/2;

            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(px - 2, py - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);

            if (currentPlayer.isAttacking()) {
                shapeRenderer.setColor(0.95f, 0.4f, 0.3f, 1);
            } else {
                shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 1);
            }
            shapeRenderer.rect(px, py, PLAYER_SIZE, PLAYER_SIZE);
        }

        // Desenhar hitbox (DENTRO DO shapeRenderer)
        renderAttackHitbox();

        shapeRenderer.end();

        // ==================== SEGUNDO SHAPERENDERER PARA LINHAS/EFEITOS ====================
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        // Desenhar bordas das barras de vida (linhas)
        renderHealthBars();

        // Desenhar efeitos de ataque (flechas, espadas, etc.)
        if (attackEffectManager != null) {
            attackEffectManager.renderLines(shapeRenderer);
        }

        shapeRenderer.end();

        // ==================== TERCEIRO SHAPERENDERER PARA PREENCHIMENTO DOS EFEITOS ====================
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        if (attackEffectManager != null) {
            attackEffectManager.renderFilled(shapeRenderer);
        }

        shapeRenderer.end();

        // ==================== RENDERIZAÇÃO DOS PROJÉTEIS (CAMADA SUPERIOR) ====================
        // PROJÉTEIS DEVEM SER RENDERIZADOS POR ÚLTIMO, EM CIMA DE TUDO
        batch.begin();

        if (projectileAnimRenderer != null) {
            projectileAnimRenderer.render(batch, gameCamera);
        }

        batch.end();

        // ==================== COOLDOWN DE ATAQUE ====================
        renderAttackCooldown();

        // ==================== UI ====================
        if (playerUI != null && Gdx.input.getInputProcessor() != playerUI.getStage()) {
            Gdx.input.setInputProcessor(createInputProcessor());
        }

        if (playerUI != null) {
            playerUI.render(batch);
        }

        if (itemRenderer != null) {
            itemRenderer.update(delta);
        }
    }

    private void renderAttackHitbox() {
        if (attackHitboxRenderer != null && attackHitboxRenderer.isActive()) {
            attackHitboxRenderer.render(shapeRenderer);
        }
    }

    @Override
    public void resize(int width, int height) {
        if (gameCamera != null) {
            gameCamera.resize(width, height);
        }
        if (playerUI != null) {
            playerUI.resize(width, height);
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (playerUI != null) playerUI.dispose();
        if (itemRenderer != null) itemRenderer.dispose();
        for (Texture t : spritesheets.values()) t.dispose();
        logger.info("GameWorldRenderer disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}