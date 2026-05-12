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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.common.sandbox.model.MapJSON;
import com.common.sandbox.model.Player;
import com.common.sandbox.model.Chunk;
import com.common.sandbox.model.WorldTile;
import com.common.sandbox.network.packets.ChatMessage;
import com.common.sandbox.network.packets.MovementBroadcast;
import com.common.sandbox.network.packets.PlayerLeftPacket;
import com.sandbox.client.editor.MapEditorScreen;
import com.sandbox.client.ui.PlayerUI;
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
    private OrthographicCamera camera;
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

    private Stage uiStage;
    private Skin skin;
    private final StringBuilder chatDisplay;

    private PlayerUI playerUI;
    private boolean initialized = false;
    private boolean spritesheetsLoaded = false;
    private Set<String> pendingSpritesheets = new HashSet<>();
    private MapJSON pendingMap = null;
    private final Map<String, Player> initialNearbyPlayers;

    public GameWorldRenderer(SandboxClient game, boolean adminMode, Map<String, Player> nearbyPlayers) {
        this.game = game;
        this.adminMode = adminMode;
        this.initialNearbyPlayers = nearbyPlayers != null ? nearbyPlayers : new HashMap<>();
        this.otherPlayers = new ConcurrentHashMap<>();
        this.loadedChunks = new ConcurrentHashMap<>();
        this.spritesheets = new HashMap<>();
        this.spritesheetRegions = new HashMap<>();
        this.chatDisplay = new StringBuilder();

        logger.info("GameWorldRenderer created with {} initial nearby players", this.initialNearbyPlayers.size());
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
        chatDisplay.append("*** Press ENTER to chat | H to hide chat | C for attributes ***\n\n");
        if (adminMode) {
            chatDisplay.append("*** ADMIN MODE: Press F12 for Map Editor ***\n");
        }

        if (playerUI != null) {
            playerUI.update(currentPlayer, 1.0f);
            playerUI.updateChatHistory(chatDisplay.toString());

            // Send all status to UI
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

        if (camera != null) {
            camera.position.set(currentPlayer.getX(), currentPlayer.getY(), 0);
            camera.update();
        }
    }

    private void setupCallbacks() {
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
        logger.info("Callbacks configured");
    }

    private void onMovementBroadcast(MovementBroadcast broadcast) {
        Gdx.app.postRunnable(() -> {
            if (broadcast.player != null) {
                if (currentPlayer != null && broadcast.player.getId().equals(currentPlayer.getId())) {
                    // Update own player directly to avoid lag
                    currentPlayer.setX(broadcast.player.getX());
                    currentPlayer.setY(broadcast.player.getY());
                    currentPlayer.setDirection(broadcast.player.getDirection());
                } else {
                    // Update other players with interpolation
                    Player existing = otherPlayers.get(broadcast.player.getId());
                    if (existing != null) {
                        // Save previous position for interpolation
                        Player interpolated = new Player();
                        interpolated.setId(existing.getId());
                        interpolated.setUsername(existing.getUsername());
                        interpolated.setX(existing.getX());
                        interpolated.setY(existing.getY());
                        interpolated.setDirection(existing.getDirection());
                        interpolatedPlayers.put(broadcast.player.getId(), interpolated);
                    }

                    // Update current position
                    otherPlayers.put(broadcast.player.getId(), broadcast.player);
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

            String[] lines = chatDisplay.toString().split("\n");
            if (lines.length > 100) {
                int firstNewline = chatDisplay.indexOf("\n");
                if (firstNewline > 0) {
                    chatDisplay.delete(0, firstNewline + 1);
                }
            }

            if (playerUI != null) {
                playerUI.updateChatHistory(chatDisplay.toString());
                playerUI.addChatMessage(formattedMsg);
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

    @Override
    public void show() {
        logger.info("GameWorldRenderer show()");

        // FIRST: create camera and batch
        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // SECOND: create font
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

        // FOURTH: create PlayerUI (skin is ready)
        createPlayerUI();

        // FIFTH: setup callbacks
        setupCallbacks();

        // SIXTH: add initial players
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

        // SEVENTH: load map
        game.getNetworkClient().requestMapLoad("11111111-1111-1111-1111-111111111111");
    }

    private void createPlayerUI() {
        playerUI = new PlayerUI();
        playerUI.setChatInputProcessor(chatInput -> {
            String message = chatInput.trim();
            if (!message.isEmpty() && currentPlayer != null) {
                game.getNetworkClient().sendChat(currentPlayer.getId(), currentPlayer.getUsername(), message);
            }
        });

        // Adicionar mensagens iniciais ao chat
        playerUI.updateChatHistory(chatDisplay.toString());

        // Adicionar mensagens de boas-vindas
        playerUI.addChatMessage("*** Welcome to Sandbox Experiment! ***");
        playerUI.addChatMessage("*** Use WASD to move ***");
        playerUI.addChatMessage("*** Press ENTER to chat | H to hide chat | C for attributes ***");

        if (adminMode) {
            playerUI.addChatMessage("*** ADMIN MODE: Press F12 for Map Editor ***");
        }

        // IMPORTANTE: Usar o InputProcessor personalizado que lida com C e ENTER
        Gdx.input.setInputProcessor(createInputProcessor());

        // Atualizar o uiStage para referenciar o stage do PlayerUI
        uiStage = playerUI.getStage();

        playerUI.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private InputProcessor createInputProcessor() {
        return new com.badlogic.gdx.InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                // Verificar se o chat está focado
                boolean chatFocused = playerUI != null && playerUI.isChatFocused();

                // Tecla C - abrir janela de atributos (somente se chat NÃO estiver focado)
                if (keycode == Input.Keys.C && !chatFocused) {
                    if (playerUI != null) {
                        logger.info("C key pressed - toggling attributes");
                        playerUI.toggleAttributes();
                    }
                    return true;
                }

                // Tecla H - esconder/mostrar chat (somente se chat NÃO estiver focado)
                if (keycode == Input.Keys.H && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleChat();
                    }
                    return true;
                }

                // Tecla ENTER
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

                // Tecla ESC
                if (keycode == Input.Keys.ESCAPE) {
                    if (playerUI != null && playerUI.isChatFocused()) {
                        playerUI.unfocusChat();
                        return true;
                    }
                    if (playerUI != null && playerUI.isAttributesVisible()) {
                        playerUI.hideAttributes();
                        return true;
                    }
                }

                // Se o chat está focado, passar TODAS as teclas para o stage do PlayerUI
                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyDown(keycode);
                }

                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                boolean chatFocused = playerUI != null && playerUI.isChatFocused();
                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyUp(keycode);
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                boolean chatFocused = playerUI != null && playerUI.isChatFocused();
                if (chatFocused && playerUI != null && playerUI.getStage() != null) {
                    return playerUI.getStage().keyTyped(character);
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                if (playerUI != null && playerUI.isChatFocused()) {
                    if (playerUI.isPointOverChat(screenX, screenY)) {
                        if (playerUI.getStage() != null) {
                            return playerUI.getStage().touchDown(screenX, screenY, pointer, button);
                        }
                    } else {
                        playerUI.unfocusChat();
                        return false;
                    }
                }
                return false;
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                if (playerUI != null && playerUI.isChatFocused() && playerUI.getStage() != null) {
                    return playerUI.getStage().touchUp(screenX, screenY, pointer, button);
                }
                return false;
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                if (playerUI != null && playerUI.isChatFocused() && playerUI.getStage() != null) {
                    return playerUI.getStage().touchDragged(screenX, screenY, pointer);
                }
                return false;
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return false;
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                return false;
            }

            @Override
            public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                return false;
            }
        };
    }

    private void renderChunks() {
        if (currentPlayer == null) return;
        if (spritesheets.isEmpty()) return;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

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

        batch.end();
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
                Chunk chunk = loadedChunks.get(chunkX + ":" + chunkY);
                if (chunk != null) {
                    WorldTile tile = chunk.getTile(localX, localY);
                    if (tile != null && tile.isSolid()) {
                        return true;
                    }
                }
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

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Player player : otherPlayers.values()) {
            float renderX = player.getX();
            float renderY = player.getY();

            // Smooth interpolation
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

            // Shadow
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, PLAYER_SIZE + 4, PLAYER_SIZE + 4);
            // Player
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

    private void handleInput(float delta) {
        if (playerUI != null && playerUI.isChatFocused()) return;
        if (currentPlayer == null) return;

        float baseSpeed = 400f;
        float terrainSpeed = getCurrentTerrainSpeed();
        float speed = baseSpeed * terrainSpeed * delta;

        if (speed <= 0.01f) speed = baseSpeed * delta;

        float moveX = 0, moveY = 0;
        String newDirection = currentPlayer.getDirection();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { moveY += 1; newDirection = "UP"; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { moveY -= 1; newDirection = "DOWN"; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { moveX -= 1; newDirection = "LEFT"; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { moveX += 1; newDirection = "RIGHT"; }

        if (moveX != 0 || moveY != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveY * moveY);
            if (length > 0) {
                moveX = moveX / length;
                moveY = moveY / length;
            }

            float newX = currentPlayer.getX() + moveX * speed;
            float newY = currentPlayer.getY() + moveY * speed;

            if (!isColliding(newX, currentPlayer.getY())) {
                currentPlayer.setX(newX);
            }
            if (!isColliding(currentPlayer.getX(), newY)) {
                currentPlayer.setY(newY);
            }

            if (!newDirection.equals(currentPlayer.getDirection())) {
                currentPlayer.setDirection(newDirection);
            }

            game.getNetworkClient().sendMovement(
                    currentPlayer.getId(),
                    currentPlayer.getX(),
                    currentPlayer.getY(),
                    currentPlayer.getDirection()
            );
        }

        if (playerUI != null && currentPlayer != null) {
            playerUI.update(currentPlayer, terrainSpeed);

            // Update real-time status
            float healthPercent = (float) currentPlayer.getCurrentHp() / currentPlayer.getMaxHp() * 100;
            float manaPercent = (float) currentPlayer.getCurrentMana() / currentPlayer.getMaxMana() * 100;
            float staminaPercent = (float) currentPlayer.getCurrentStamina() / currentPlayer.getMaxStamina() * 100;

            playerUI.setHealth(healthPercent);
            playerUI.setMana(manaPercent);
            playerUI.setStamina(staminaPercent);
            playerUI.setGold(currentPlayer.getGold());
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
            camera.position.set(currentPlayer.getX(), currentPlayer.getY(), 0);
            camera.update();
        }

        // 1. RENDER CHUNKS (uses batch)
        renderChunks();

        // 2. RENDER PLAYERS (uses shapeRenderer)
        renderPlayers();

        // 3. RENDER NAMES (uses batch)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        renderFloatingNames();
        batch.end();

        if (playerUI != null) {
            playerUI.render(batch);
        }
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
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