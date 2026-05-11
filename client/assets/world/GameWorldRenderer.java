package com.sandbox.client;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.common.sandbox.model.Player;
import com.common.sandbox.model.Chunk;
import com.common.sandbox.model.WorldTile;
import com.common.sandbox.network.packets.ChatMessage;
import com.common.sandbox.network.packets.MovementBroadcast;
import com.sandbox.client.editor.MapEditorScreen;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameWorldRenderer implements Screen {
    private static final Logger logger = LoggerFactory.getLogger(GameWorldRenderer.class);
    private static final int TILE_SIZE = 32;
    private static final int WORLD_TILE_SIZE = 64;
    private static final int CHUNK_SIZE = 32;

    private final SandboxClient game;
    private final boolean adminMode;
    private OrthographicCamera camera;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;

    private Player currentPlayer;
    private final Map<String, Player> otherPlayers;
    private final Map<String, Chunk> loadedChunks;
    
    // Spritesheets carregados dos assets
    private final Map<String, Texture> spritesheets;
    private final Map<String, TextureRegion[][]> spritesheetRegions;

    // UI
    private Stage uiStage;
    private Skin skin;
    private Label chatLabel;
    private TextField chatInput;
    private ScrollPane chatScrollPane;
    private final StringBuilder chatDisplay;

    private boolean initialized = false;

    public GameWorldRenderer(SandboxClient game, boolean adminMode) {
        this.game = game;
        this.adminMode = adminMode;
        this.otherPlayers = new ConcurrentHashMap<>();
        this.loadedChunks = new ConcurrentHashMap<>();
        this.spritesheets = new HashMap<>();
        this.spritesheetRegions = new HashMap<>();
        this.chatDisplay = new StringBuilder();

        logger.info("GameWorldRenderer created");
        loadSpritesheets();
    }
    
    private void loadSpritesheets() {
        // Carregar spritesheets do diretório common/assets/world/
        String[] spritesheetNames = {"default", "outside"};
        
        for (String name : spritesheetNames) {
            try {
                String path = "world/" + name + ".png";
                if (Gdx.files.internal(path).exists()) {
                    Texture texture = new Texture(Gdx.files.internal(path));
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    spritesheets.put(name, texture);
                    
                    int cols = texture.getWidth() / TILE_SIZE;
                    int rows = texture.getHeight() / TILE_SIZE;
                    TextureRegion[][] regions = TextureRegion.split(texture, TILE_SIZE, TILE_SIZE);
                    spritesheetRegions.put(name, regions);
                    
                    logger.info("Loaded spritesheet: {} ({}x{} tiles)", name, cols, rows);
                } else {
                    logger.warn("Spritesheet not found: {}", path);
                }
            } catch (Exception e) {
                logger.error("Failed to load spritesheet: {}", name, e);
            }
        }
        
        // Se não conseguiu carregar nenhum, criar textura padrão
        if (spritesheets.isEmpty()) {
            logger.warn("No spritesheets found, creating default texture");
            Pixmap pixmap = new Pixmap(TILE_SIZE, TILE_SIZE, Pixmap.Format.RGBA8888);
            pixmap.setColor(Color.FOREST);
            pixmap.fill();
            Texture defaultTexture = new Texture(pixmap);
            pixmap.dispose();
            spritesheets.put("default", defaultTexture);
            
            TextureRegion[][] regions = TextureRegion.split(defaultTexture, TILE_SIZE, TILE_SIZE);
            spritesheetRegions.put("default", regions);
        }
    }

    public void setCurrentPlayer(Player player) {
        this.currentPlayer = player;
        logger.info("Current player: {} at ({}, {})", player.getUsername(), player.getX(), player.getY());

        // NÃO limpar otherPlayers aqui! Os jogadores já foram adicionados no onLoginResponse

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
            String formattedMsg;
            if ("SISTEMA".equals(chat.senderName)) {
                formattedMsg = String.format("*** %s ***", chat.message);
            } else {
                formattedMsg = String.format("[%s] %s: %s",
                        new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date(chat.timestamp)),
                        chat.senderName,
                        chat.message
                );
            }

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
            String key = chunk.chunkX + ":" + chunk.chunkY;
            loadedChunks.put(key, chunk);
            logger.debug("Chunk [{},{}] received", chunk.chunkX, chunk.chunkY);
        });
    }

    @Override
    public void show() {
        logger.info("GameWorldRenderer show() called");

        setupCallbacks();

        camera = new OrthographicCamera();
        camera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();

        // Load font maior para o mundo
        try {
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/arial.ttf"));
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 20;
            parameter.borderWidth = 1;
            parameter.borderColor = Color.BLACK;
            parameter.color = Color.WHITE;
            font = generator.generateFont(parameter);
            generator.dispose();
            logger.info("Font loaded successfully");
        } catch (Exception e) {
            logger.warn("Font not found, using default BitmapFont");
            font = new BitmapFont();
            font.getData().setScale(1.2f);
        }

        createSkin();
        createUI();

        initialized = true;
        logger.info("Game world started with 2x tile size ({}px)", WORLD_TILE_SIZE);
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
        textFieldStyle.cursor = buttonDrawable;
        textFieldStyle.selection = buttonDrawable;
        skin.add("default", textFieldStyle);

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = buttonDrawable;
        skin.add("default", scrollStyle);
    }

    private void createUI() {
        uiStage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(uiStage);

        // Chat container (canto inferior esquerdo)
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

        // Instruções (canto superior central - FIXO NA TELA)
        String instructions = "WASD = Movement | ENTER = Chat | ESC = Exit chat";
        if (adminMode) {
            instructions += " | F12 = Map Editor";
        }
        Label infoLabel = new Label(instructions, skin);
        infoLabel.setFontScale(0.9f);
        infoLabel.setPosition(Gdx.graphics.getWidth() / 2 - instructions.length() * 4, Gdx.graphics.getHeight() - 30);
        uiStage.addActor(infoLabel);

        // Admin mode indicator (canto superior direito)
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

        int centerChunkX = (int) Math.floor(currentPlayer.getX() / (CHUNK_SIZE * WORLD_TILE_SIZE));
        int centerChunkY = (int) Math.floor(currentPlayer.getY() / (CHUNK_SIZE * WORLD_TILE_SIZE));

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                int chunkX = centerChunkX + dx;
                int chunkY = centerChunkY + dy;
                String key = chunkX + ":" + chunkY;

                Chunk chunk = loadedChunks.get(key);
                if (chunk != null) {
                    renderChunk(chunk, chunkX, chunkY);
                }
            }
        }
    }

    private void renderChunk(Chunk chunk, int chunkX, int chunkY) {
        float worldOffsetX = chunkX * CHUNK_SIZE * WORLD_TILE_SIZE;
        float worldOffsetY = chunkY * CHUNK_SIZE * WORLD_TILE_SIZE;

        // Usar SpriteBatch para renderizar tiles (melhor performance)
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                WorldTile tile = chunk.getTile(x, y);
                
                if (tile != null && tile.tileId > 0) {
                    int actualTileId = tile.tileId % 10000;
                    int layer = tile.tileId / 10000;
                    
                    // Usar spritesheet "default" para todos os tiles (ou mapear por layer)
                    // TODO: Mapear corretamente o spritesheet baseado no tileId
                    String spritesheetName = "default";
                    
                    // Tentar carregar do spritesheet específico
                    if (actualTileId == 0) actualTileId = 1;
                    
                    TextureRegion region = getTileRegion(spritesheetName, actualTileId);
                    if (region != null) {
                        float xPos = worldOffsetX + x * WORLD_TILE_SIZE;
                        float yPos = worldOffsetY + y * WORLD_TILE_SIZE;
                        
                        // Desenhar com escala 2x (WORLD_TILE_SIZE / TILE_SIZE = 2)
                        batch.draw(region, xPos, yPos, WORLD_TILE_SIZE, WORLD_TILE_SIZE);
                    } else {
                        // Fallback: desenhar retângulo colorido
                        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
                        float r = ((actualTileId * 50) % 200) / 255f;
                        float g = ((actualTileId * 80) % 200) / 255f;
                        float b = ((actualTileId * 120) % 200) / 255f;
                        shapeRenderer.setColor(r, g, b, 1f);
                        shapeRenderer.rect(
                                worldOffsetX + x * WORLD_TILE_SIZE,
                                worldOffsetY + y * WORLD_TILE_SIZE,
                                WORLD_TILE_SIZE, WORLD_TILE_SIZE
                        );
                        shapeRenderer.end();
                    }
                }
            }
        }
        
        batch.end();
    }
    
    private TextureRegion getTileRegion(String spritesheetName, int tileId) {
        TextureRegion[][] regions = spritesheetRegions.get(spritesheetName);
        if (regions == null) {
            // Tentar "default" como fallback
            regions = spritesheetRegions.get("default");
            if (regions == null) return null;
        }
        
        int cols = regions[0].length;
        int row = tileId / cols;
        int col = tileId % cols;
        
        if (row < regions.length && col < regions[0].length) {
            return regions[row][col];
        }
        return null;
    }

    private void renderPlayers() {
        float size = 48;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        for (Player player : otherPlayers.values()) {
            float x = player.getX() - size/2;
            float y = player.getY() - size/2;
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, size + 4, size + 4);
            shapeRenderer.setColor(0.5f, 0.7f, 0.3f, 1);
            shapeRenderer.rect(x, y, size, size);
        }

        if (currentPlayer != null) {
            float x = currentPlayer.getX() - size/2;
            float y = currentPlayer.getY() - size/2;
            shapeRenderer.setColor(0, 0, 0, 0.5f);
            shapeRenderer.rect(x - 2, y - 2, size + 4, size + 4);
            shapeRenderer.setColor(0.2f, 0.6f, 0.9f, 1);
            shapeRenderer.rect(x, y, size, size);
        }
        
        shapeRenderer.end();
    }

    private void renderFloatingNames() {
        float size = 48;

        batch.setProjectionMatrix(camera.combined);
        batch.begin();

        for (Player player : otherPlayers.values()) {
            float worldX = player.getX() - (player.getUsername().length() * 5);
            float worldY = player.getY() + size/2 + 10;

            font.setColor(1f, 1f, 1f, 1f);
            font.draw(batch, player.getUsername(), worldX, worldY);

            String dirSymbol = getDirectionSymbol(player.getDirection());
            font.draw(batch, dirSymbol, player.getX() - 8, player.getY() - 25);
        }

        if (currentPlayer != null) {
            float worldX = currentPlayer.getX() - (currentPlayer.getUsername().length() * 5);
            float worldY = currentPlayer.getY() + size/2 + 10;

            font.setColor(1f, 0.9f, 0.2f, 1f);
            font.draw(batch, currentPlayer.getUsername(), worldX, worldY);

            String dirSymbol = getDirectionSymbol(currentPlayer.getDirection());
            font.draw(batch, dirSymbol, currentPlayer.getX() - 8, currentPlayer.getY() - 25);
        }

        batch.end();
    }

    private String getDirectionSymbol(String direction) {
        if (direction == null) return "●";
        switch (direction) {
            case "UP": return "▲";
            case "DOWN": return "▼";
            case "LEFT": return "◀";
            case "RIGHT": return "▶";
            default: return "●";
        }
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

            int maxWorldSize = 50 * CHUNK_SIZE * WORLD_TILE_SIZE;
            newX = Math.max(100, Math.min(maxWorldSize - 100, newX));
            newY = Math.max(100, Math.min(maxWorldSize - 100, newY));

            currentPlayer.setX(newX);
            currentPlayer.setY(newY);
            currentPlayer.setDirection(newDirection);

            game.getNetworkClient().sendMovement(currentPlayer.getId(), newX, newY, newDirection);
        }
    }

    @Override
    public void render(float delta) {
        if (!initialized) return;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Handle chat input
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (uiStage.getKeyboardFocus() == chatInput) {
                sendChatMessage();
            } else {
                uiStage.setKeyboardFocus(chatInput);
                chatInput.selectAll();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (uiStage.getKeyboardFocus() == chatInput) {
                uiStage.setKeyboardFocus(null);
                chatInput.setText("");
            }
        }

        if (adminMode && Gdx.input.isKeyJustPressed(Input.Keys.F12)) {
            MapEditorScreen editorScreen = new MapEditorScreen(game);
            game.setScreen(editorScreen);
            return;
        }

        handleInput(delta);

        if (currentPlayer != null) {
            camera.position.set(currentPlayer.getX(), currentPlayer.getY(), 0);
        }
        camera.update();

        // 1. Desenhar chunks (usando SpriteBatch dentro do método)
        renderChunks();

        // 2. Desenhar jogadores
        renderPlayers();

        // 3. Desenhar nomes flutuantes
        renderFloatingNames();

        // 4. Desenhar UI
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
        for (Texture texture : spritesheets.values()) {
            texture.dispose();
        }
        logger.info("GameWorldRenderer disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}