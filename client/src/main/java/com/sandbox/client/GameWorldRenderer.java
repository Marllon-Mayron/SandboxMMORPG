package com.sandbox.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
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
import com.sandbox.client.editor.MapEditorScreen;
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
    private Label chatLabel;
    private TextField chatInput;
    private ScrollPane chatScrollPane;
    private final StringBuilder chatDisplay;

    private boolean initialized = false;
    private boolean spritesheetsLoaded = false;
    private Set<String> pendingSpritesheets = new HashSet<>();
    private MapJSON pendingMap = null;

    public GameWorldRenderer(SandboxClient game, boolean adminMode) {
        this.game = game;
        this.adminMode = adminMode;
        this.otherPlayers = new ConcurrentHashMap<>();
        this.loadedChunks = new ConcurrentHashMap<>();
        this.spritesheets = new HashMap<>();
        this.spritesheetRegions = new HashMap<>();
        this.chatDisplay = new StringBuilder();

        logger.info("GameWorldRenderer created");
    }

    /**
     * Carrega APENAS os spritesheets necessários para o mapa (na thread principal)
     */
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

                    logger.info("✅ Loaded spritesheet: {} ({}x{} tiles)", path, cols, rows);
                } catch (Exception e) {
                    logger.error("Failed to load spritesheet: {}", path, e);
                }
            } else {
                logger.error("❌ Required spritesheet not found: {}", path);
            }
        }

        logger.info("Total spritesheets loaded: {}", spritesheets.size());
        spritesheetsLoaded = true;

        // Agora carrega o mapa no mundo
        if (pendingMap != null) {
            loadMapIntoWorld(pendingMap);
            pendingMap = null;
        }
    }

    /**
     * Coleta todos os paths de spritesheets usados no mapa
     */
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
        chatDisplay.append("*** Press ENTER to chat ***\n\n");
        if (adminMode) {
            chatDisplay.append("*** ADMIN MODE: Press F12 for Map Editor ***\n");
        }

        if (chatLabel != null) {
            chatLabel.setText(chatDisplay.toString());
            if (chatScrollPane != null) {
                chatScrollPane.setScrollPercentY(1);
            }
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

                // Coletar paths necessários
                Set<String> requiredPaths = collectSpritesheetPaths(response.mapJson);

                // Guardar o mapa para processar depois que os spritesheets carregarem
                pendingMap = response.mapJson;
                spritesheetsLoaded = false;

                // Carregar spritesheets na thread principal
                Gdx.app.postRunnable(() -> {
                    loadRequiredSpritesheets(requiredPaths, response.mapJson);
                });
            }
        });
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

            if (chatLabel != null) {
                chatLabel.setText(chatDisplay.toString());
                if (chatScrollPane != null) {
                    chatScrollPane.setScrollPercentY(1);
                }
            }
        });
    }

    private void onChunkReceived(Chunk chunk) {
        Gdx.app.postRunnable(() -> {
            loadedChunks.put(chunk.chunkX + ":" + chunk.chunkY, chunk);
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

                            // O campo 'solid' é ignorado, usamos a tag
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

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 20;
            parameter.borderWidth = 1;
            parameter.borderColor = Color.BLACK;
            parameter.color = Color.WHITE;
            font = generator.generateFont(parameter);
            generator.dispose();
        } catch (Exception e) {
            font = new BitmapFont();
            font.getData().setScale(1.2f);
        }

        createSkin();
        createUI();
        initialized = true;

        // Request map - isso vai trigger o callback e carregar os spritesheets na thread principal
        game.getNetworkClient().requestMapLoad("11111111-1111-1111-1111-111111111111");
    }

    private void createSkin() {
        skin = new Skin();
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(0.15f, 0.15f, 0.2f, 0.9f);
        pixmap.fill();
        Texture buttonTexture = new Texture(pixmap);
        pixmap.dispose();
        Drawable buttonDrawable = new TextureRegionDrawable(buttonTexture);

        Label.LabelStyle labelStyle = new Label.LabelStyle();
        labelStyle.font = font;
        labelStyle.fontColor = Color.WHITE;
        skin.add("default", labelStyle);

        TextButton.TextButtonStyle textButtonStyle = new TextButton.TextButtonStyle();
        textButtonStyle.font = font;
        textButtonStyle.fontColor = Color.WHITE;
        textButtonStyle.up = buttonDrawable;
        skin.add("default", textButtonStyle);

        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = buttonDrawable;
        skin.add("default", textFieldStyle);

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = buttonDrawable;
        skin.add("default", scrollStyle);
    }

    private void createUI() {
        uiStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(uiStage);

        Table chatContainer = new Table(skin);
        chatContainer.setWidth(500);
        chatContainer.setHeight(350);
        chatContainer.setPosition(10, 10);

        chatLabel = new Label("", skin);
        chatLabel.setWrap(true);
        chatLabel.setAlignment(Align.topLeft);
        chatLabel.setWidth(480);

        chatScrollPane = new ScrollPane(chatLabel, skin);
        chatScrollPane.setFadeScrollBars(false);
        chatScrollPane.setScrollingDisabled(true, false);
        chatScrollPane.setSize(490, 280);

        chatInput = new TextField("", skin);
        chatInput.setMessageText("Press ENTER to chat...");
        chatInput.setSize(420, 35);

        TextButton sendButton = new TextButton("Send", skin);
        sendButton.setSize(70, 35);
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendChatMessage();
            }
        });

        chatContainer.add(chatScrollPane).width(490).height(280).padBottom(5);
        chatContainer.row();

        Table inputTable = new Table();
        inputTable.add(chatInput).width(420).padRight(5);
        inputTable.add(sendButton).width(70);
        chatContainer.add(inputTable).width(500);
        uiStage.addActor(chatContainer);

        String instructions = "WASD = Movement | ENTER = Chat | ESC = Exit chat";
        if (adminMode) instructions += " | F12 = Map Editor";

        Label infoLabel = new Label(instructions, skin);
        infoLabel.setFontScale(0.9f);
        infoLabel.setPosition(Gdx.graphics.getWidth() / 2 - instructions.length() * 4, Gdx.graphics.getHeight() - 30);
        uiStage.addActor(infoLabel);

        if (adminMode) {
            Label adminLabel = new Label("ADMIN MODE", skin);
            adminLabel.setColor(Color.GOLD);
            adminLabel.setFontScale(0.8f);
            adminLabel.setPosition(Gdx.graphics.getWidth() - 120, Gdx.graphics.getHeight() - 25);
            uiStage.addActor(adminLabel);
        }
    }

    private void sendChatMessage() {
        String message = chatInput.getText().trim();
        if (!message.isEmpty() && currentPlayer != null) {
            game.getNetworkClient().sendChat(currentPlayer.getId(), currentPlayer.getUsername(), message);
            chatInput.setText("");
            uiStage.setKeyboardFocus(null);
        }
    }

    private void renderChunks() {
        if (currentPlayer == null) return;
        if (spritesheets.isEmpty()) {
            return;
        }

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
                if (tile != null && tile.tileId != 0 && tile.spritesheetPath != null && !tile.spritesheetPath.isEmpty()) {
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

        if (regions == null) {
            return null;
        }

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

        // Verificar os 4 cantos do jogador
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
                    // SÓ VERIFICA SE É SÓLIDO BASEADO NA TAG
                    if (tile != null && tile.isSolid()) {
                        return true;
                    }
                }
            }
        }
        return false;
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
                    player.getY() + PLAYER_SIZE/2 + 15);
        }

        if (currentPlayer != null) {
            font.setColor(1f, 0.9f, 0.2f, 1f);
            font.draw(batch, currentPlayer.getUsername(),
                    currentPlayer.getX() - (currentPlayer.getUsername().length() * 5),
                    currentPlayer.getY() + PLAYER_SIZE/2 + 15);
        }
        batch.end();
    }

    private void handleInput(float delta) {
        if (uiStage.getKeyboardFocus() == chatInput) return;
        if (currentPlayer == null) return;

        float speed = 400f * delta;
        float moveX = 0, moveY = 0;
        String newDirection = currentPlayer.getDirection();

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { moveY += speed; newDirection = "UP"; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { moveY -= speed; newDirection = "DOWN"; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { moveX -= speed; newDirection = "LEFT"; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { moveX += speed; newDirection = "RIGHT"; }

        if (moveX != 0 || moveY != 0) {
            float newX = currentPlayer.getX() + moveX;
            float newY = currentPlayer.getY() + moveY;

            if (!isColliding(newX, newY)) {
                currentPlayer.setX(newX);
                currentPlayer.setY(newY);
                currentPlayer.setDirection(newDirection);
                game.getNetworkClient().sendMovement(currentPlayer.getId(), newX, newY, newDirection);
            } else {
                if (!isColliding(newX, currentPlayer.getY())) {
                    currentPlayer.setX(newX);
                    game.getNetworkClient().sendMovement(currentPlayer.getId(), newX, currentPlayer.getY(), newDirection);
                } else if (!isColliding(currentPlayer.getX(), newY)) {
                    currentPlayer.setY(newY);
                    game.getNetworkClient().sendMovement(currentPlayer.getId(), currentPlayer.getX(), newY, newDirection);
                }
            }
        }
    }

    @Override
    public void render(float delta) {
        if (!initialized) return;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (uiStage.getKeyboardFocus() == chatInput) {
                sendChatMessage();
            } else {
                uiStage.setKeyboardFocus(chatInput);
                chatInput.selectAll();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && uiStage.getKeyboardFocus() == chatInput) {
            uiStage.setKeyboardFocus(null);
            chatInput.setText("");
        }

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

        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        camera.viewportWidth = width;
        camera.viewportHeight = height;
        camera.update();
        if (uiStage != null) {
            uiStage.getViewport().update(width, height, true);
            for (Actor actor : uiStage.getActors()) {
                if (actor instanceof Label) {
                    String text = ((Label) actor).getText().toString();
                    if (text.contains("WASD")) {
                        actor.setPosition(width / 2 - text.length() * 4, height - 30);
                    } else if (text.equals("ADMIN MODE")) {
                        actor.setPosition(width - 120, height - 25);
                    }
                }
            }
        }
    }

    @Override
    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (uiStage != null) uiStage.dispose();
        if (skin != null) skin.dispose();
        for (Texture t : spritesheets.values()) t.dispose();
        logger.info("GameWorldRenderer disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}