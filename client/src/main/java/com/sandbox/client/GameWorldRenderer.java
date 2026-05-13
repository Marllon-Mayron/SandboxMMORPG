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
import com.common.sandbox.model.MapJSON;
import com.common.sandbox.model.Player;
import com.common.sandbox.model.Chunk;
import com.common.sandbox.model.WorldTile;
import com.common.sandbox.network.packets.*;
import com.sandbox.client.editor.MapEditorScreen;
import com.sandbox.client.input.PlayerInputManager;
import com.sandbox.client.renderer.ItemRenderer;
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
    private static final long MOVEMENT_SEND_INTERVAL_MS = 50; // 20 packets por segundo
    private long lastStatusSyncTime = 0;
    private static final long STATUS_SYNC_INTERVAL_MS = 10000; // 10 segundos

    private ItemRenderer itemRenderer;

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

        // ⭐ CONFIGURAR CALLBACKS IMEDIATAMENTE NO CONSTRUTOR
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

        // ===== Inicializar ItemRenderer =====
        itemRenderer = new ItemRenderer();

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

    private void loadRequiredSpritesheets(Set<String> requiredPaths, MapJSON mapJson) {
        logger.info("Loading required spritesheets (on main thread): {}", requiredPaths);

        for (String path : requiredPaths) {
            FileHandle file = Gdx.files.internal(path);
            if (!file.exists()) {
                file = Gdx.files.internal("assets/" + path);
            }
            if (!file.exists()) {
                file = Gdx.files.internal("client/assets/" + path);
            }
            if (!file.exists()) {
                file = Gdx.files.absolute("C:/Users/Marllon/IdeaProjects/sandbox-simulator/client/assets/" + path);
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

                    logger.info("Loaded spritesheet: {} ({}x{} tiles)", path, cols, rows);
                } catch (Exception e) {
                    logger.error("Failed to load spritesheet: {}", path, e);
                }
            } else {
                logger.error("Required spritesheet not found: {}", path);
            }
        }

        logger.info("Total spritesheets loaded: {}", spritesheets.size());
        spritesheetsLoaded = true;

        if (pendingMap != null) {
            loadMapIntoWorld(pendingMap);
            pendingMap = null;
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

        logger.info("Spritesheets required by map: {}", paths);
        return paths;
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        logger.info("Current player: {} at ({}, {})", player.getUsername(), player.getX(), player.getY());

        chatDisplay.append("*** Welcome to Sandbox Experiment! ***\n");
        chatDisplay.append("*** Use WASD to move ***\n");
        chatDisplay.append("*** SHIFT to sprint | SPACE to dash ***\n");
        chatDisplay.append("*** Press ENTER to chat | H to hide chat | C for attributes ***\n\n");
        if (adminMode) {
            chatDisplay.append("*** ADMIN MODE: Press F12 for Map Editor ***\n");
        }

        if (playerUI != null) {
            playerUI.update(currentPlayer, 1.0f);
            playerUI.updateChatHistory(chatDisplay.toString());

            if (player.getMaxHp() > 0) {
                float healthPercent = (float) player.getCurrentHp() / player.getMaxHp() * 100;
                playerUI.setHealth(healthPercent);
            }
            if (player.getMaxMana() > 0) {
                float manaPercent = (float) player.getCurrentMana() / player.getMaxMana() * 100;
                playerUI.setMana(manaPercent);
            }
            if (player.getMaxStamina() > 0) {
                float staminaPercent = (float) player.getCurrentStamina() / player.getMaxStamina() * 100;
                playerUI.setStamina(staminaPercent);
            }

            playerUI.setGold(player.getGold());
        }

        if (gameCamera != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.resetPosition();
        }
    }

    private void setupCallbacks() {
        logger.info("Setting up callbacks for GameWorldRenderer");

        game.getNetworkClient().setMovementCallback(this::onMovementBroadcast);
        game.getNetworkClient().setChatCallback(this::onChatMessage);
        game.getNetworkClient().setChunkCallback(this::onChunkReceived);
        game.getNetworkClient().setMapLoadCallback(response -> {
            if (response.success && response.mapJson != null) {
                logger.info("Map loaded, processing...");

                Set<String> requiredPaths = collectSpritesheetPaths(response.mapJson);
                pendingMap = response.mapJson;
                spritesheetsLoaded = false;

                Gdx.app.postRunnable(() -> {
                    loadRequiredSpritesheets(requiredPaths, response.mapJson);
                });
            }
        });
        game.getNetworkClient().setPlayerLeftCallback(this::onPlayerLeft);

        game.getNetworkClient().setFriendListCallback(this::onFriendListResponse);
        game.getNetworkClient().setFriendRequestCallback(this::onFriendRequestResponse);
        game.getNetworkClient().setPrivateMessageCallback(this::onPrivateMessage);
        game.getNetworkClient().setPrivateMessageHistoryCallback(this::onPrivateMessageHistory);

        game.getNetworkClient().setItemSpawnCallback(this::onItemSpawn);
        game.getNetworkClient().setItemDespawnCallback(this::onItemDespawn);

        logger.info("Callbacks configured");
    }

    private void onMovementBroadcast(MovementBroadcast broadcast) {
        Gdx.app.postRunnable(() -> {
            if (broadcast.player != null) {
                if (currentPlayer != null && broadcast.player.getId().equals(currentPlayer.getId())) {
                    // Apenas atualizar posição e direção
                    currentPlayer.setX(broadcast.player.getX());
                    currentPlayer.setY(broadcast.player.getY());
                    currentPlayer.setDirection(broadcast.player.getDirection());
                    // NÃO atualizar HP, Mana, Stamina!
                } else {
                    Player existing = otherPlayers.get(broadcast.player.getId());
                    if (existing != null) {
                        // Apenas atualizar posição e direção do player existente
                        existing.setX(broadcast.player.getX());
                        existing.setY(broadcast.player.getY());
                        existing.setDirection(broadcast.player.getDirection());

                        // Interpolação
                        Player interpolated = new Player();
                        interpolated.setId(existing.getId());
                        interpolated.setUsername(existing.getUsername());
                        interpolated.setX(existing.getX());
                        interpolated.setY(existing.getY());
                        interpolated.setDirection(existing.getDirection());
                        interpolatedPlayers.put(broadcast.player.getId(), interpolated);
                    } else {
                        otherPlayers.put(broadcast.player.getId(), broadcast.player);
                    }
                    lastUpdateTime.put(broadcast.player.getId(), System.currentTimeMillis());
                }
            }
        });
    }

    private void onPlayerLeft(PlayerLeftPacket packet) {
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

    private void onChatMessage(ChatMessage chat) {
        Gdx.app.postRunnable(() -> {
            String formattedMsg = "SISTEMA".equals(chat.senderName)
                    ? String.format("*** %s ***", chat.message)
                    : String.format("[%s] %s: %s",
                    new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(chat.timestamp)),
                    chat.senderName, chat.message);

            chatDisplay.append(formattedMsg).append("\n");

            // Truncar se necessário
            String[] lines = chatDisplay.toString().split("\n");
            if (lines.length > 100) {
                int firstNewline = chatDisplay.indexOf("\n");
                if (firstNewline > 0) {
                    chatDisplay.delete(0, firstNewline + 1);
                }
            }

            if (playerUI != null) {
                // APENAS atualizar o histórico, NÃO adicionar mensagem separadamente
                playerUI.updateChatHistory(chatDisplay.toString());
            }
        });
    }

    private void onChunkReceived(Chunk chunk) {
        Gdx.app.postRunnable(() -> {
            String key = chunk.chunkX + ":" + chunk.chunkY;
            loadedChunks.put(key, chunk);
            logger.debug("Chunk [{},{}] received", chunk.chunkX, chunk.chunkY);
        });
    }

    private void onPrivateMessageHistory(PrivateMessageHistoryResponse response) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.loadPrivateChatHistory(response.friendId, response.messages);
            }
        });
    }

    private void onFriendListResponse(FriendListResponse response) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.updateFriendsList(response);
            }
        });
    }

    private void onFriendRequestResponse(FriendRequestPacket packet) {
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

    private void onPrivateMessage(PrivateMessagePacket packet) {
        Gdx.app.postRunnable(() -> {
            if (playerUI != null) {
                playerUI.addPrivateMessage(packet.fromUsername, packet.message, packet.timestamp);
                playerUI.addChatMessage("[Privado] " + packet.fromUsername + ": " + packet.message);
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

    public void loadMapIntoWorld(MapJSON map) {
        loadedChunks.clear();
        logger.info("Loading map with {} chunks", map.getChunks().size());

        int totalTiles = 0;

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
                            if (path == null || path.isEmpty()) {
                                logger.warn("Tile at [{},{}] layer{} has no spritesheet path", chunkX, chunkY, layer);
                                continue;
                            }

                            int tileId = tileData.getTileId();
                            String tag = tileData.getTag();

                            if (tileId < 0) continue;

                            boolean isSolidTile = "solid".equals(tag);

                            chunk.setTile(x, y, path, tileId, isSolidTile, tag);
                            totalTiles++;
                        }
                    }
                }
            }
            loadedChunks.put(entry.getKey(), chunk);
            logger.debug("Loaded chunk [{},{}]", chunkX, chunkY);
        }
        logger.info("Total tiles loaded: {}", totalTiles);
    }

    // ==================== SETUP DO INPUT MANAGER ====================

    private void setupPlayerInput() {
        playerInputManager = new PlayerInputManager();

        playerInputManager.setOnDash(() -> {
            if (currentPlayer != null && currentPlayer.canDash()) {
                if (currentPlayer.executeDash()) {
                    // Iniciar dash
                    isDashing = true;
                    dashTimer = Player.getDashDuration();
                    dashStartPos.set(currentPlayer.getX(), currentPlayer.getY());
                    dashDirection.set(playerInputManager.getDashDirection());

                    // Atualizar UI da stamina
                    if (playerUI != null) {
                        float staminaPercent = (float) currentPlayer.getCurrentStamina() / currentPlayer.getMaxStamina() * 100;
                        playerUI.setStamina(staminaPercent);
                    }

                    logger.debug("Dash executed by {}", currentPlayer.getUsername());
                }
            }
        });

        playerInputManager.setOnSprintStart(() -> {
            // Opcional: efeito sonoro ou visual
            logger.trace("Sprint started");
        });

        playerInputManager.setOnSprintEnd(() -> {
            // Opcional: efeito sonoro ou visual
            logger.trace("Sprint ended");
        });

        logger.info("PlayerInputManager initialized");
    }

    @Override
    public void show() {
        logger.info("GameWorldRenderer show()");
        if (!initialized) {
            init();
        }
    }

    public void onItemSpawn(ItemSpawnPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (itemRenderer != null && packet.item != null) {
                itemRenderer.addItem(packet.item);
                logger.info("Item spawned in world: {} at ({}, {})",
                        packet.item.getDefinition().getName(),
                        packet.item.getX(),
                        packet.item.getY());
            }
        });
    }

    public void onItemDespawn(ItemDespawnPacket packet) {
        Gdx.app.postRunnable(() -> {
            if (itemRenderer != null) {
                itemRenderer.removeItem(packet.instanceId);
                logger.debug("Item despawned: {}", packet.instanceId);
            }
        });
    }

    private void createPlayerUI() {
        playerUI = new PlayerUI();

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

        playerUI.updateChatHistory(chatDisplay.toString());

        playerUI.setOnLoadPrivateChatHistory(friendId -> {
            if (currentPlayer != null) {
                logger.info("Requesting private message history for friend: {}", friendId);
                game.getNetworkClient().requestPrivateMessageHistory(friendId, 100);
            }
        });

        playerUI.addChatMessage("*** Welcome to Sandbox Experiment! ***");
        playerUI.addChatMessage("*** Use WASD to move ***");
        playerUI.addChatMessage("*** SHIFT to sprint | SPACE to dash ***");
        playerUI.addChatMessage("*** Press ENTER to chat | H to hide chat | C for attributes ***");
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
                // Primeiro, passar para o PlayerInputManager
                if (inputManager != null && inputManager.keyDown(keycode)) {
                    return true;
                }

                // IMPORTANTE: Se o chat privado está visível, passar TODAS as teclas para ele
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

    // ==================== MÉTODOS DE MOVIMENTO ====================

    private void handleInput(float delta) {
        // Verificar se UI está bloqueando input
        if (playerUI != null && (playerUI.isChatFocused() ||
                playerUI.isFriendsWindowVisible() ||
                playerUI.isAttributesVisible() ||
                playerUI.isPrivateChatVisible())) {
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

        // Regeneração
        currentPlayer.updateRegeneration(delta);
        if (playerUI != null) {
            playerUI.setStamina(currentPlayer.getStaminaPercentage() * 100);
            playerUI.setHealth(currentPlayer.getHpPercentage() * 100);
            playerUI.setMana(currentPlayer.getManaPercentage() * 100);
        }

        // Dash
        if (isDashing) {
            handleDash(delta);
            return;
        }

        // Verificar se o jogador acabou de executar um dash
        if (playerInputManager.isDashJustExecuted()) {
            if (!isDashing && currentPlayer.getDashTimer() > 0) {
                isDashing = true;
                dashTimer = currentPlayer.getDashTimer();
                dashStartPos.set(currentPlayer.getX(), currentPlayer.getY());
                dashDirection.set(playerInputManager.getDashDirection());
            }
            return;
        }

        // Movimento normal
        Vector2 dir = playerInputManager.getMovementDirection();
        boolean sprinting = playerInputManager.isSprinting();

        // ==================== CALCULAR SPEED MULTIPLIER ====================
        float playerSpeedMultiplier = 1.0f;
        boolean moved = false;

        if (dir.x != 0 || dir.y != 0) {
            moved = true;
            float baseSpeed = Player.getBaseSpeed();
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

        // ==================== ENVIAR MOVIMENTO E STATUS PARA O SERVIDOR ====================
        long now = System.currentTimeMillis();

        // Enviar sempre que houver movimento OU a cada 10 segundos (sync de status)
        boolean shouldSend = moved;
        boolean timeForSync = (now - lastStatusSyncTime) >= STATUS_SYNC_INTERVAL_MS;

        if (shouldSend || timeForSync) {
            // Usar rate limiting para movimento (max 20 packets/segundo = 50ms)
            if (moved && (now - lastMovementSendTime) < MOVEMENT_SEND_INTERVAL_MS) {
                // Muito cedo para enviar movimento, mas se for sync, pode enviar mesmo assim
                if (!timeForSync) {
                    // Não enviar nada ainda
                } else {
                    // Enviar sync mesmo que esteja no rate limit
                    sendMovementWithStatus();
                    lastStatusSyncTime = now;
                }
            } else {
                // Enviar pacote completo
                sendMovementWithStatus();
                if (moved) {
                    lastMovementSendTime = now;
                }
                if (timeForSync) {
                    lastStatusSyncTime = now;
                }
            }
        }

        // ==================== ATUALIZAR UI ====================
        if (playerUI != null) {
            playerUI.setSpeedMultiplier(playerSpeedMultiplier);
            playerUI.update(currentPlayer, getCurrentTerrainSpeed());
        }
    }

    // Método auxiliar para enviar movimento com status
    private void sendMovementWithStatus() {
        if (currentPlayer == null || game.getNetworkClient() == null) return;

        MovementRequest request = new MovementRequest(
                currentPlayer.getId(),
                currentPlayer.getX(),
                currentPlayer.getY(),
                currentPlayer.getDirection()
        );
        // Preencher os status
        request.currentHp = currentPlayer.getCurrentHp();
        request.currentMana = currentPlayer.getCurrentMana();
        request.currentStamina = currentPlayer.getCurrentStamina();
        request.currentGold = currentPlayer.getGold();
        request.currentExperience = currentPlayer.getExperience();
        request.currentLevel = currentPlayer.getLevel();

        game.getNetworkClient().sendPacket(request);
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
            // Parar o dash se colidir
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

            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);
            shapeRenderer.setColor(0.5f, 0.7f, 0.3f, 1);
            shapeRenderer.rect(x, y, PLAYER_SIZE, PLAYER_SIZE);
        }

        if (currentPlayer != null) {
            float x = currentPlayer.getX() - PLAYER_SIZE/2;
            float y = currentPlayer.getY() - PLAYER_SIZE/2;
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);
            shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 1);
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

            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, player.getUsername(),
                    renderX - (player.getUsername().length() * 5),
                    renderY + PLAYER_SIZE/2 + 25);
        }

        if (currentPlayer != null) {
            font.setColor(1f, 0.9f, 0.2f, 1f);
            font.draw(batch, currentPlayer.getUsername(),
                    currentPlayer.getX() - (currentPlayer.getUsername().length() * 5),
                    currentPlayer.getY() + PLAYER_SIZE/2 + 25);
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

        if (currentPlayer != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.update(delta);
        }

        // INICIAR O BATCH APENAS UMA VEZ
        batch.setProjectionMatrix(gameCamera.getCamera().combined);
        batch.begin();

        // 1. Renderizar chunks (tiles)
        renderChunks();

        // 2. Renderizar itens (acima dos tiles)
        if (itemRenderer != null) {
            itemRenderer.render(batch, gameCamera);
        }

        // 3. Renderizar nomes flutuantes (acima dos itens)
        renderFloatingNames();

        // FECHAR O BATCH
        batch.end();

        // 4. Renderizar players (ShapeRenderer, não precisa de batch)
        renderPlayers();

        // Configurar o input processor da UI
        if (playerUI != null && Gdx.input.getInputProcessor() != playerUI.getStage()) {
            Gdx.input.setInputProcessor(createInputProcessor());
        }

        if (playerUI != null) {
            playerUI.render(batch);
        }

        // Atualizar a animação dos itens (fora do batch)
        if (itemRenderer != null) {
            itemRenderer.update(delta);
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
        for (Texture t : spritesheets.values()) t.dispose();
        logger.info("GameWorldRenderer disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}