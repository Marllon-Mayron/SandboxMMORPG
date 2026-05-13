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
import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.*;
import com.common.sandbox.network.packets.InventoryUpdatePacket;
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
    private static final float PICKUP_RANGE = 48f; // Raio para pegar itens

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

        // ⭐ CARREGAR SPRITESHEET DOS ITENS PRIMEIRO
        loadItemSpritesheet();

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

    private void loadItemSpritesheet() {
        String path = "itens/spritesheet_itens.png";

        // Tentar diferentes caminhos
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
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            spritesheets.put(loadedPath, texture);

            int cols = texture.getWidth() / TILE_SIZE;
            int rows = texture.getHeight() / TILE_SIZE;
            TextureRegion[][] regions = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
            spritesheetRegions.put(loadedPath, regions);

            logger.info("✅ Loaded item spritesheet: {} ({}x{} tiles, {}x{} pixels)",
                    loadedPath, cols, rows, texture.getWidth(), texture.getHeight());
        } catch (Exception e) {
            logger.error("Failed to load item spritesheet: {}", loadedPath, e);
        }
    }

    private void createPlaceholderSpritesheet(String path) {
        Pixmap pixmap = new Pixmap(64, 64, Pixmap.Format.RGBA8888);

        // Tile 0,0 - Maçã (vermelho)
        pixmap.setColor(0.8f, 0.2f, 0.2f, 1f);
        pixmap.fillRectangle(0, 32, 32, 32);

        // Tile 1,0 - Espada (cinza)
        pixmap.setColor(0.5f, 0.5f, 0.5f, 1f);
        pixmap.fillRectangle(0, 0, 32, 32);

        // Tile 1,1 - Poção (azul)
        pixmap.setColor(0.2f, 0.2f, 0.8f, 1f);
        pixmap.fillRectangle(32, 0, 32, 32);

        // Tile 0,1 - Vazio/Default
        pixmap.setColor(0.3f, 0.3f, 0.3f, 1f);
        pixmap.fillRectangle(32, 32, 32, 32);

        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        spritesheets.put(path, texture);

        int cols = texture.getWidth() / TILE_SIZE;
        int rows = texture.getHeight() / TILE_SIZE;
        TextureRegion[][] regions = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
        spritesheetRegions.put(path, regions);

        logger.info("Created placeholder spritesheet: {} ({}x{} tiles)", path, cols, rows);
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
    private void registerItemTextures() {
        if (playerUI == null) return;

        // Para cada item que foi spawnado, registrar sua textura
        for (GroundItem item : localGroundItems.values()) {
            String itemId = item.getDefinition().getId();
            String spritesheetPath = item.getDefinition().getSpritesheet();
            int tileX = item.getDefinition().getTileX();
            int tileY = item.getDefinition().getTileY();

            TextureRegion[][] regions = spritesheetRegions.get(spritesheetPath);
            if (regions != null && tileY < regions.length && tileX < regions[0].length) {
                TextureRegion icon = regions[tileY][tileX];
                playerUI.registerItemTexture(itemId, icon, item.getDefinition());
                logger.info("Registered item texture for: {} - {}", itemId, item.getDefinition().getName());
            }
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
        chatDisplay.append("*** Press ENTER to chat | H to hide chat | C for attributes | I for inventory ***\n\n");
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

            // ⭐ REGISTRAR ITENS DO INVENTÁRIO DO JOGADOR
            registerInventoryItems();

            playerUI.updateInventory(player.getInventory(), player.getGold());
        }

        if (gameCamera != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.resetPosition();
        }
    }

    /**
     * Registra os itens do inventário do jogador no InventoryWindow
     */
    private void registerInventoryItems() {
        if (currentPlayer == null || playerUI == null) return;

        Inventory inventory = currentPlayer.getInventory();
        if (inventory == null) {
            logger.warn("Inventory is null for player: {}", currentPlayer.getUsername());
            return;
        }

        logger.info("=== REGISTERING INVENTORY ITEMS ===");
        logger.info("Player: {}", currentPlayer.getUsername());
        logger.info("Total slots in inventory: {}", inventory.getSlots().size());
        logger.info("Equipped items: {}", inventory.getEquipped().size());

        // Registrar itens nos slots do inventário
        for (ItemStack stack : inventory.getSlots().values()) {
            if (stack != null && !stack.isEmpty()) {
                String itemId = stack.getItemId();
                logger.info("Found item in inventory slot {}: {}", stack.getSlot(), itemId);

                // Tentar encontrar a definição nos itens já spawnados
                ItemDefinition def = findItemDefinition(itemId);

                if (def != null) {
                    logger.info("  - Definition found: Name={}, Category={}", def.getName(), def.getCategory());
                    TextureRegion icon = getItemTextureFromDefinition(def);
                    playerUI.registerItemTexture(itemId, icon, def);
                } else {
                    logger.warn("  - Definition NOT found for item: {}", itemId);
                }
            }
        }

        // Registrar itens equipados
        for (Map.Entry<String, String> entry : inventory.getEquipped().entrySet()) {
            String slotType = entry.getKey();
            String itemId = entry.getValue();
            if (itemId != null && !itemId.isEmpty()) {
                logger.info("Found equipped item: {} in slot: {}", itemId, slotType);

                ItemDefinition def = findItemDefinition(itemId);
                if (def != null) {
                    TextureRegion icon = getItemTextureFromDefinition(def);
                    playerUI.registerItemTexture(itemId, icon, def);
                }
            }
        }

        logger.info("=== END REGISTERING INVENTORY ITEMS ===");
    }

    /**
     * Busca a definição de um item pelos itens já spawnados no mundo
     */
    private ItemDefinition findItemDefinition(String itemId) {
        // Primeiro, tentar encontrar nos itens locais já spawnados
        for (GroundItem groundItem : localGroundItems.values()) {
            if (groundItem.getDefinition().getId().equals(itemId)) {
                logger.debug("Found definition for {} from localGroundItems", itemId);
                return groundItem.getDefinition();
            }
        }

        // Se não encontrou, tentar criar uma definição básica baseada no ID
        // Isso é útil para itens que já estão no inventário mas não foram spawnados recentemente
        logger.warn("Item definition not found in localGroundItems for: {}, creating placeholder", itemId);
        return createPlaceholderDefinition(itemId);
    }

    /**
     * Cria uma definição placeholder para itens que não foram encontrados
     */
    private ItemDefinition createPlaceholderDefinition(String itemId) {
        ItemDefinition def = new ItemDefinition();
        def.setId(itemId);

        // Nome baseado no ID
        String name = itemId;
        switch (itemId) {
            case "simple_sword":
                name = "Espada Simples";
                def.setCategory("weapon");
                def.setTileX(1);
                def.setTileY(0);
                break;
            case "apple":
                name = "Maçã";
                def.setCategory("consumable");
                def.setTileX(0);
                def.setTileY(0);
                break;
            case "health_potion":
                name = "Poção de Vida";
                def.setCategory("consumable");
                def.setTileX(2);
                def.setTileY(0);
                break;
            default:
                name = itemId;
                def.setCategory("common");
                def.setTileX(0);
                def.setTileY(0);
                break;
        }

        def.setName(name);
        def.setSpritesheet("itens/spritesheet_itens.png");

        logger.info("Created placeholder definition for {}: Name={}, Category={}, Tile=({},{})",
                itemId, name, def.getCategory(), def.getTileX(), def.getTileY());

        return def;
    }

    /**
     * Obtém a textura do item a partir da definição
     */
    private TextureRegion getItemTextureFromDefinition(ItemDefinition def) {
        String spritesheetPath = def.getSpritesheet();
        TextureRegion[][] regions = spritesheetRegions.get(spritesheetPath);

        if (regions == null) {
            logger.warn("Spritesheet not loaded: {}, trying to load", spritesheetPath);
            loadItemSpritesheet();
            regions = spritesheetRegions.get(spritesheetPath);
        }

        if (regions != null) {
            int tileX = def.getTileX();
            int tileY = def.getTileY();
            if (tileY < regions.length && tileX < regions[0].length) {
                logger.debug("Texture found for {} at ({},{})", def.getId(), tileX, tileY);
                return regions[tileY][tileX];
            } else {
                logger.warn("Invalid tile coordinates for {}: ({},{}) max ({},{})",
                        def.getId(), tileX, tileY, regions[0].length, regions.length);
            }
        } else {
            logger.warn("Spritesheet still not loaded: {}", spritesheetPath);
        }

        return null;
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

        logger.info("Callbacks configured");
    }
    // ==================== CALLBACKS ====================

    public void onMovementBroadcast(MovementBroadcast broadcast) {
        Gdx.app.postRunnable(() -> {
            if (broadcast.player != null) {
                if (currentPlayer != null && broadcast.player.getId().equals(currentPlayer.getId())) {
                    currentPlayer.setX(broadcast.player.getX());
                    currentPlayer.setY(broadcast.player.getY());
                    currentPlayer.setDirection(broadcast.player.getDirection());
                } else {
                    Player existing = otherPlayers.get(broadcast.player.getId());
                    if (existing != null) {
                        existing.setX(broadcast.player.getX());
                        existing.setY(broadcast.player.getY());
                        existing.setDirection(broadcast.player.getDirection());

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

                // ⭐ IMPORTANTE: Registrar a definição do item no inventário
                if (playerUI != null) {
                    String itemId = packet.item.getDefinition().getId();
                    String category = packet.item.getDefinition().getCategory();
                    String spritesheetPath = packet.item.getDefinition().getSpritesheet();
                    int tileX = packet.item.getDefinition().getTileX();
                    int tileY = packet.item.getDefinition().getTileY();

                    logger.info("=== REGISTERING ITEM DEFINITION ===");
                    logger.info("Item ID: {}", itemId);
                    logger.info("Item Name: {}", packet.item.getDefinition().getName());
                    logger.info("Category: {}", category);
                    logger.info("Spritesheet: {}", spritesheetPath);
                    logger.info("Tile: ({}, {})", tileX, tileY);

                    // Buscar a textura do spritesheet
                    TextureRegion[][] regions = spritesheetRegions.get(spritesheetPath);
                    TextureRegion icon = null;

                    if (regions != null && tileY < regions.length && tileX < regions[0].length) {
                        icon = regions[tileY][tileX];
                        logger.info("✅ Texture found for item: {}", itemId);
                    } else {
                        logger.warn("❌ Texture NOT found for item: {} - regions available: {}",
                                itemId, regions != null ? regions.length : 0);
                    }

                    // Registrar no PlayerUI (que vai passar para o InventoryWindow)
                    playerUI.registerItemTexture(itemId, icon, packet.item.getDefinition());
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

                // ⭐ REGISTRAR ITENS DO INVENTÁRIO ATUALIZADO
                for (ItemStack stack : packet.inventory.getSlots().values()) {
                    if (stack != null && !stack.isEmpty()) {
                        String itemId = stack.getItemId();
                        // Verificar se já está registrado
                        if (playerUI != null && !playerUI.isItemRegistered(itemId)) {
                            // Tentar encontrar a definição
                            for (GroundItem groundItem : localGroundItems.values()) {
                                if (groundItem.getDefinition().getId().equals(itemId)) {
                                    ItemDefinition def = groundItem.getDefinition();
                                    String spritesheetPath = def.getSpritesheet();
                                    TextureRegion[][] regions = spritesheetRegions.get(spritesheetPath);
                                    TextureRegion icon = null;

                                    if (regions != null) {
                                        int tileX = def.getTileX();
                                        int tileY = def.getTileY();
                                        if (tileY < regions.length && tileX < regions[0].length) {
                                            icon = regions[tileY][tileX];
                                        }
                                    }

                                    playerUI.registerItemTexture(itemId, icon, def);
                                    logger.info("Registered item from inventory update: {} - Category: {}", itemId, def.getCategory());
                                    break;
                                }
                            }
                        }
                    }
                }

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
                    isDashing = true;
                    dashTimer = Player.getDashDuration();
                    dashStartPos.set(currentPlayer.getX(), currentPlayer.getY());
                    dashDirection.set(playerInputManager.getDashDirection());

                    if (playerUI != null) {
                        float staminaPercent = (float) currentPlayer.getCurrentStamina() / currentPlayer.getMaxStamina() * 100;
                        playerUI.setStamina(staminaPercent);
                    }

                    logger.debug("Dash executed by {}", currentPlayer.getUsername());
                }
            }
        });

        playerInputManager.setOnSprintStart(() -> {
            logger.trace("Sprint started");
        });

        playerInputManager.setOnSprintEnd(() -> {
            logger.trace("Sprint ended");
        });

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

    // ==================== MÉTODOS DE MOVIMENTO ====================

    private void handleInput(float delta) {
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

        currentPlayer.updateRegeneration(delta);
        if (playerUI != null) {
            playerUI.setStamina(currentPlayer.getStaminaPercentage() * 100);
            playerUI.setHealth(currentPlayer.getHpPercentage() * 100);
            playerUI.setMana(currentPlayer.getManaPercentage() * 100);
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

        long now = System.currentTimeMillis();
        boolean shouldSend = moved;
        boolean timeForSync = (now - lastStatusSyncTime) >= STATUS_SYNC_INTERVAL_MS;

        if (shouldSend || timeForSync) {
            if (moved && (now - lastMovementSendTime) < MOVEMENT_SEND_INTERVAL_MS) {
                if (!timeForSync) {
                    // Não enviar ainda
                } else {
                    sendMovementWithStatus();
                    lastStatusSyncTime = now;
                }
            } else {
                sendMovementWithStatus();
                if (moved) {
                    lastMovementSendTime = now;
                }
                if (timeForSync) {
                    lastStatusSyncTime = now;
                }
            }
        }

        if (playerUI != null) {
            playerUI.setSpeedMultiplier(playerSpeedMultiplier);
            playerUI.update(currentPlayer, getCurrentTerrainSpeed());
        }
    }

    private void sendMovementWithStatus() {
        if (currentPlayer == null || game.getNetworkClient() == null) return;

        MovementRequest request = new MovementRequest(
                currentPlayer.getId(),
                currentPlayer.getX(),
                currentPlayer.getY(),
                currentPlayer.getDirection()
        );
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
                logger.info("Unequipping item from slot index: {}", slotIndex);

                // Mapear slot index para tipo de equipamento
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
                    logger.info("Dropping item: {} x{} from slot {}",
                            stack.getItemId(), action.quantity, action.slot);

                    DropItemPacket packet = new DropItemPacket(action.slot, action.quantity, currentPlayer.getId());
                    game.getNetworkClient().sendPacket(packet);

                    // Feedback visual
                    playerUI.addChatMessage("Item dropped: " + stack.getItemId() + " x" + action.quantity);
                }
            }
        });

        playerUI.updateChatHistory(chatDisplay.toString());

        playerUI.addChatMessage("*** Welcome to Sandbox Experiment! ***");
        playerUI.addChatMessage("*** Use WASD to move ***");
        playerUI.addChatMessage("*** SHIFT to sprint | SPACE to dash ***");
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

        if (currentPlayer != null) {
            gameCamera.setTarget(currentPlayer.getX(), currentPlayer.getY());
            gameCamera.update(delta);
        }

        batch.setProjectionMatrix(gameCamera.getCamera().combined);
        batch.begin();

        renderChunks();

        if (itemRenderer != null) {
            itemRenderer.render(batch, gameCamera);
        }

        renderFloatingNames();

        batch.end();

        renderPlayers();

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