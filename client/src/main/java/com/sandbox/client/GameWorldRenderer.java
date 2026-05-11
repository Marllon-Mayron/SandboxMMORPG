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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
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
        chatDisplay.append("*** Press ENTER to chat | H to hide chat ***\n\n");
        if (adminMode) {
            chatDisplay.append("*** ADMIN MODE: Press F12 for Map Editor ***\n");
        }

        if (playerUI != null) {
            playerUI.update(currentPlayer, 1.0f);
            playerUI.updateChatHistory(chatDisplay.toString());
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
                    currentPlayer.setX(broadcast.player.getX());
                    currentPlayer.setY(broadcast.player.getY());
                    currentPlayer.setDirection(broadcast.player.getDirection());
                } else {
                    otherPlayers.put(broadcast.player.getId(), broadcast.player);
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
        setupCallbacks();

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

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

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

        createSkin();
        createPlayerUI();

        initialized = true;
        game.getNetworkClient().requestMapLoad("11111111-1111-1111-1111-111111111111");
    }

    private void createSkin() {
        skin = new Skin();
        skin.add("default-font", font);

        // Criar drawables básicos
        Pixmap darkPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        darkPixmap.setColor(0.12f, 0.12f, 0.16f, 0.95f);
        darkPixmap.fill();
        Texture darkTexture = new Texture(darkPixmap);
        darkPixmap.dispose();
        Drawable darkDrawable = new TextureRegionDrawable(darkTexture);

        Pixmap lightPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        lightPixmap.setColor(0.22f, 0.22f, 0.28f, 0.95f);
        lightPixmap.fill();
        Texture lightTexture = new Texture(lightPixmap);
        lightPixmap.dispose();
        Drawable lightDrawable = new TextureRegionDrawable(lightTexture);

        Pixmap buttonPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        buttonPixmap.setColor(0.2f, 0.2f, 0.25f, 1);
        buttonPixmap.fill();
        Texture buttonTexture = new Texture(buttonPixmap);
        buttonPixmap.dispose();
        Drawable buttonDrawable = new TextureRegionDrawable(buttonTexture);

        Pixmap greenPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        greenPixmap.setColor(0.2f, 0.6f, 0.2f, 1);
        greenPixmap.fill();
        Texture greenTexture = new Texture(greenPixmap);
        greenPixmap.dispose();
        Drawable greenDrawable = new TextureRegionDrawable(greenTexture);

        Pixmap redPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        redPixmap.setColor(0.7f, 0.2f, 0.2f, 1);
        redPixmap.fill();
        Texture redTexture = new Texture(redPixmap);
        redPixmap.dispose();
        Drawable redDrawable = new TextureRegionDrawable(redTexture);

        Pixmap bluePixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        bluePixmap.setColor(0.2f, 0.4f, 0.7f, 1);
        bluePixmap.fill();
        Texture blueTexture = new Texture(bluePixmap);
        bluePixmap.dispose();
        Drawable blueDrawable = new TextureRegionDrawable(blueTexture);

        Pixmap goldPixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        goldPixmap.setColor(0.8f, 0.6f, 0.1f, 1);
        goldPixmap.fill();
        Texture goldTexture = new Texture(goldPixmap);
        goldPixmap.dispose();
        Drawable goldDrawable = new TextureRegionDrawable(goldTexture);

        // Registrar drawables
        skin.add("window-bg", darkDrawable);
        skin.add("button-bg", buttonDrawable);
        skin.add("green", greenDrawable);
        skin.add("red", redDrawable);
        skin.add("blue", blueDrawable);
        skin.add("gold", goldDrawable);

        // Label styles
        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        Label.LabelStyle titleStyle = new Label.LabelStyle();
        titleStyle.font = font;
        titleStyle.fontColor = Color.GOLD;
        skin.add("title", titleStyle);

        Label.LabelStyle statusStyle = new Label.LabelStyle();
        statusStyle.font = font;
        statusStyle.fontColor = Color.LIGHT_GRAY;
        skin.add("status", statusStyle);

        // TextButton style
        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = buttonDrawable;
        textButtonStyle.down = darkDrawable;
        textButtonStyle.over = blueDrawable;
        skin.add("default", textButtonStyle);

        TextButton.TextButtonStyle successStyle = new TextButton.TextButtonStyle();
        successStyle.font = font;
        successStyle.fontColor = Color.WHITE;
        successStyle.up = greenDrawable;
        successStyle.down = darkDrawable;
        successStyle.over = blueDrawable;
        skin.add("success", successStyle);

        TextButton.TextButtonStyle primaryStyle = new TextButton.TextButtonStyle();
        primaryStyle.font = font;
        primaryStyle.fontColor = Color.WHITE;
        primaryStyle.up = blueDrawable;
        primaryStyle.down = darkDrawable;
        primaryStyle.over = buttonDrawable;
        skin.add("primary", primaryStyle);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = buttonDrawable;
        textFieldStyle.cursor = blueDrawable;
        textFieldStyle.selection = blueDrawable;
        skin.add("default", textFieldStyle);

        // ScrollPane style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = darkDrawable;
        scrollStyle.vScroll = buttonDrawable;
        scrollStyle.vScrollKnob = blueDrawable;
        scrollStyle.hScroll = buttonDrawable;
        scrollStyle.hScrollKnob = blueDrawable;
        skin.add("default", scrollStyle);
    }

    private void createPlayerUI() {
        playerUI = new PlayerUI(skin);
        playerUI.setChatInputProcessor(chatInput -> {
            String message = chatInput.trim();
            if (!message.isEmpty() && currentPlayer != null) {
                game.getNetworkClient().sendChat(currentPlayer.getId(), currentPlayer.getUsername(), message);
            }
        });

        uiStage = playerUI.getStage();
        Gdx.input.setInputProcessor(createInputProcessor());
    }

    private InputProcessor createInputProcessor() {
        return new com.badlogic.gdx.InputProcessor() {
            @Override
            public boolean keyDown(int keycode) {
                // Verificar se o chat está focado
                boolean chatFocused = playerUI != null && playerUI.isChatFocused();

                // Tecla H - só esconde/mostra chat se o chat NÃO estiver focado
                if (keycode == Input.Keys.H && !chatFocused) {
                    if (playerUI != null) {
                        playerUI.toggleChat();
                    }
                    return true; // CONSUMIR - não passar para ninguém
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
                    return true; // CONSUMIR
                }

                // Tecla ESC
                if (keycode == Input.Keys.ESCAPE) {
                    if (playerUI != null && playerUI.isChatFocused()) {
                        playerUI.unfocusChat();
                        return true;
                    }
                }

                // Se o chat está focado, passar TODAS as teclas para o stage (incluindo H)
                if (chatFocused) {
                    return uiStage != null && uiStage.keyDown(keycode);
                }

                // Se o chat NÃO está focado, não passar teclas para o stage
                // As teclas de movimento são tratadas no handleInput
                return false;
            }

            @Override
            public boolean keyUp(int keycode) {
                if (playerUI != null && playerUI.isChatFocused()) {
                    return uiStage != null && uiStage.keyUp(keycode);
                }
                return false;
            }

            @Override
            public boolean keyTyped(char character) {
                if (playerUI != null && playerUI.isChatFocused()) {
                    return uiStage != null && uiStage.keyTyped(character);
                }
                return false;
            }

            @Override
            public boolean touchDown(int screenX, int screenY, int pointer, int button) {
                // Se clicar fora do chat quando estiver focado, perder foco
                if (playerUI != null && playerUI.isChatFocused()) {
                    if (playerUI.isPointOverChat(screenX, screenY)) {
                        return uiStage != null && uiStage.touchDown(screenX, screenY, pointer, button);
                    } else {
                        playerUI.unfocusChat();
                        return false;
                    }
                }
                return uiStage != null && uiStage.touchDown(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchUp(int screenX, int screenY, int pointer, int button) {
                return uiStage != null && uiStage.touchUp(screenX, screenY, pointer, button);
            }

            @Override
            public boolean touchDragged(int screenX, int screenY, int pointer) {
                return uiStage != null && uiStage.touchDragged(screenX, screenY, pointer);
            }

            @Override
            public boolean mouseMoved(int screenX, int screenY) {
                return uiStage != null && uiStage.mouseMoved(screenX, screenY);
            }

            @Override
            public boolean scrolled(float amountX, float amountY) {
                return uiStage != null && uiStage.scrolled(amountX, amountY);
            }

            @Override
            public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
                return uiStage != null && uiStage.touchCancelled(screenX, screenY, pointer, button);
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
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (Player player : otherPlayers.values()) {
            float x = player.getX() - PLAYER_SIZE/2;
            float y = player.getY() - PLAYER_SIZE/2;
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
        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (Player player : otherPlayers.values()) {
            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, player.getUsername(),
                    player.getX() - (player.getUsername().length() * 5),
                    player.getY() + PLAYER_SIZE/2 + 25);
        }

        if (currentPlayer != null) {
            font.setColor(1f, 0.9f, 0.2f, 1f);
            font.draw(batch, currentPlayer.getUsername(),
                    currentPlayer.getX() - (currentPlayer.getUsername().length() * 5),
                    currentPlayer.getY() + PLAYER_SIZE/2 + 25);
        }
        batch.end();
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

        renderChunks();
        renderPlayers();
        renderFloatingNames();

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