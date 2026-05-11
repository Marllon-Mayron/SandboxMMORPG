package com.sandbox.client.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.common.sandbox.model.MapJSON;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.core.EditorCamera;
import com.sandbox.client.editor.core.EditorInputHandler;
import com.sandbox.client.editor.core.EditorRenderer;
import com.sandbox.client.editor.core.InputMultiplexer;
import com.sandbox.client.editor.models.*;
import com.sandbox.client.editor.ui.EditorWindow;
import com.sandbox.client.editor.utils.MapExporter;
import com.common.sandbox.network.packets.MapLoadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapEditorScreen implements Screen {
    private static final Logger logger = LoggerFactory.getLogger(MapEditorScreen.class);

    private final SandboxClient game;
    private Stage uiStage;
    private Skin skin;
    private EditorState state;
    private EditorCamera editorCamera;
    private EditorRenderer renderer;
    private EditorInputHandler inputHandler;
    private EditorWindow editorWindow;
    private MapExporter mapExporter;

    private boolean initialized = false;
    private boolean loadingMap = true;

    public MapEditorScreen(SandboxClient game) {
        this.game = game;
        this.mapExporter = new MapExporter(game);
    }

    @Override
    public void show() {
        state = new EditorState();

        editorCamera = new EditorCamera();
        editorCamera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        uiStage = new Stage(new ScreenViewport());
        createSkin();

        editorWindow = new EditorWindow(skin, game, editorCamera, state, mapExporter);
        renderer = new EditorRenderer(editorCamera, editorWindow, state);
        inputHandler = new EditorInputHandler(editorCamera, editorWindow, renderer, state);

        uiStage.addActor(editorWindow.getWindow());

        InputMultiplexer multiplexer = new InputMultiplexer(uiStage, inputHandler);
        Gdx.input.setInputProcessor(multiplexer);

        setupCallbacks();

        loadMapFromServer();
    }

    private void setupCallbacks() {
        game.getNetworkClient().setMapSaveCallback(response -> {
            Gdx.app.postRunnable(() -> {
                if (response.success) {
                    logger.info("✅ Map saved successfully: {}", response.message);
                } else {
                    logger.error("❌ Map save failed: {}", response.message);
                }
            });
        });
    }

    private void loadMapFromServer() {
        logger.info("Loading map from server...");
        game.getNetworkClient().setMapLoadCallback(this::onMapLoaded);
        game.getNetworkClient().requestMapLoad("11111111-1111-1111-1111-111111111111");
    }

    private void onMapLoaded(MapLoadResponse response) {
        Gdx.app.postRunnable(() -> {
            if (response.success && response.mapJson != null) {
                logger.info("Map loaded successfully with {} chunks", response.mapJson.getChunks().size());

                // Primeiro carrega o mapa e coleta os paths dos spritesheets usados
                Set<String> requiredSpritesheets = loadChunksAndCollectSpritesheets(response.mapJson);

                // Depois carrega APENAS os spritesheets necessários
                loadRequiredSpritesheets(requiredSpritesheets);

                // Agora sim, carrega os tiles no editor
                loadChunksIntoEditor(response.mapJson);
            } else {
                logger.error("Failed to load map from server: {}", response.message);
                state.createChunk(0, 0);
                state.setCurrentChunk(0, 0);
            }

            if (editorWindow != null) {
                editorWindow.refreshTilePalette();
                editorWindow.refreshUI();
            }

            editorCamera.centerOnChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
            editorCamera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            loadingMap = false;
            initialized = true;
            logger.info("Map Editor ready - spritesheets: {}", state.getSpritesheets().keySet());
        });
    }

    /**
     * Carrega o mapa e coleta todos os paths de spritesheets usados
     */
    private Set<String> loadChunksAndCollectSpritesheets(MapJSON mapJson) {
        Set<String> spritesheetPaths = new HashSet<>();

        for (Map.Entry<String, MapJSON.ChunkData> entry : mapJson.getChunks().entrySet()) {
            MapJSON.ChunkData jsonChunk = entry.getValue();

            for (int layer = 0; layer < 3; layer++) {
                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        MapJSON.TileData tileData = jsonChunk.getTile(layer, x, y);
                        if (tileData != null && !tileData.isEmpty()) {
                            String path = tileData.getSpritesheetPath();
                            if (path != null && !path.isEmpty()) {
                                spritesheetPaths.add(path);
                            }
                        }
                    }
                }
            }
        }

        logger.info("Spritesheets used in map: {}", spritesheetPaths);
        return spritesheetPaths;
    }

    /**
     * Carrega APENAS os spritesheets necessários para o mapa
     */
    private void loadRequiredSpritesheets(Set<String> requiredPaths) {
        logger.info("Loading required spritesheets...");
        state.getSpritesheets().clear();

        for (String path : requiredPaths) {
            FileHandle file = Gdx.files.internal(path);
            if (!file.exists()) {
                // Tentar caminhos alternativos
                file = Gdx.files.internal("assets/" + path);
                if (!file.exists()) {
                    file = Gdx.files.internal("client/assets/" + path);
                }
            }

            if (file.exists()) {
                try {
                    Texture texture = new Texture(file);
                    texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                    String name = file.nameWithoutExtension();
                    SpritesheetData sheet = new SpritesheetData(name, path, texture);
                    state.getSpritesheets().put(path, sheet);
                    logger.info("✅ Loaded required spritesheet: {} -> {}", name, path);
                } catch (Exception e) {
                    logger.error("Failed to load required spritesheet: {}", path, e);
                }
            } else {
                logger.error("❌ Required spritesheet not found: {}", path);
                logger.error("   Please ensure the file exists and restart the editor");
            }
        }

        if (!state.getSpritesheets().isEmpty()) {
            SpritesheetData first = state.getSpritesheets().values().iterator().next();
            state.setCurrentSpritesheet(first);
            logger.info("Selected first spritesheet: {} ({})", first.getName(), first.getPath());
        }
    }

    private void loadChunksIntoEditor(MapJSON mapJson) {
        state.getChunks().clear();

        logger.info("Loading {} chunks from JSON map", mapJson.getChunks().size());

        for (Map.Entry<String, MapJSON.ChunkData> entry : mapJson.getChunks().entrySet()) {
            String[] coords = entry.getKey().split(":");
            int chunkX = Integer.parseInt(coords[0]);
            int chunkY = Integer.parseInt(coords[1]);

            MapJSON.ChunkData jsonChunk = entry.getValue();

            state.createChunk(chunkX, chunkY);
            ChunkData editorChunk = state.getChunks().get(chunkX + ":" + chunkY);

            if (editorChunk != null) {
                int tilesLoaded = 0;

                for (int layer = 0; layer < 3; layer++) {
                    for (int x = 0; x < 32; x++) {
                        for (int y = 0; y < 32; y++) {
                            MapJSON.TileData tileData = jsonChunk.getTile(layer, x, y);

                            if (tileData != null && !tileData.isEmpty()) {
                                String spritesheetPath = tileData.getSpritesheetPath();
                                int tileId = tileData.getTileId();

                                if (state.getSpritesheets().containsKey(spritesheetPath)) {
                                    editorChunk.setTile(LayerType.fromId(layer), x, y, spritesheetPath, tileId);
                                    tilesLoaded++;
                                } else {
                                    logger.warn("Spritesheet '{}' not loaded, tile will not appear", spritesheetPath);
                                }
                            }
                        }
                    }
                }
                logger.info("Loaded chunk [{},{}] with {} tiles", chunkX, chunkY, tilesLoaded);
            }
        }

        if (state.getChunks().isEmpty()) {
            logger.info("No chunks in map, creating empty chunk [0,0]");
            state.createChunk(0, 0);
            state.setCurrentChunk(0, 0);
        } else {
            ChunkData firstChunk = state.getChunks().values().iterator().next();
            state.setCurrentChunk(firstChunk.getX(), firstChunk.getY());
            logger.info("Selected current chunk: [{},{}]", firstChunk.getX(), firstChunk.getY());
        }
    }

    public void refreshTilePalette() {
        if (editorWindow != null) {
            editorWindow.refreshTilePalette();
        }
    }

    private void createSkin() {
        skin = new Skin();

        com.badlogic.gdx.graphics.g2d.BitmapFont font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.getData().setScale(0.9f);

        com.badlogic.gdx.graphics.Pixmap darkPixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        darkPixmap.setColor(0.12f, 0.12f, 0.16f, 1);
        darkPixmap.fill();
        com.badlogic.gdx.graphics.Texture darkTexture = new com.badlogic.gdx.graphics.Texture(darkPixmap);
        darkPixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable darkDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(darkTexture);

        com.badlogic.gdx.graphics.Pixmap lightPixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        lightPixmap.setColor(0.22f, 0.22f, 0.28f, 1);
        lightPixmap.fill();
        com.badlogic.gdx.graphics.Texture lightTexture = new com.badlogic.gdx.graphics.Texture(lightPixmap);
        lightPixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable lightDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(lightTexture);

        com.badlogic.gdx.graphics.Pixmap greenPixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        greenPixmap.setColor(0.2f, 0.6f, 0.2f, 1);
        greenPixmap.fill();
        com.badlogic.gdx.graphics.Texture greenTexture = new com.badlogic.gdx.graphics.Texture(greenPixmap);
        greenPixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable greenDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(greenTexture);

        com.badlogic.gdx.graphics.Pixmap redPixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        redPixmap.setColor(0.7f, 0.2f, 0.2f, 1);
        redPixmap.fill();
        com.badlogic.gdx.graphics.Texture redTexture = new com.badlogic.gdx.graphics.Texture(redPixmap);
        redPixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable redDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(redTexture);

        com.badlogic.gdx.graphics.Pixmap bluePixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        bluePixmap.setColor(0.2f, 0.4f, 0.7f, 1);
        bluePixmap.fill();
        com.badlogic.gdx.graphics.Texture blueTexture = new com.badlogic.gdx.graphics.Texture(bluePixmap);
        bluePixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable blueDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(blueTexture);

        com.badlogic.gdx.graphics.Pixmap orangePixmap = new com.badlogic.gdx.graphics.Pixmap(1, 1, com.badlogic.gdx.graphics.Pixmap.Format.RGBA8888);
        orangePixmap.setColor(0.8f, 0.5f, 0.2f, 1);
        orangePixmap.fill();
        com.badlogic.gdx.graphics.Texture orangeTexture = new com.badlogic.gdx.graphics.Texture(orangePixmap);
        orangePixmap.dispose();
        com.badlogic.gdx.scenes.scene2d.utils.Drawable orangeDrawable = new com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable(orangeTexture);

        // Label styles
        Label.LabelStyle defaultLabel = new Label.LabelStyle();
        defaultLabel.font = font;
        defaultLabel.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", defaultLabel);

        Label.LabelStyle titleLabel = new Label.LabelStyle();
        titleLabel.font = font;
        titleLabel.fontColor = com.badlogic.gdx.graphics.Color.GOLD;
        skin.add("title", titleLabel);

        Label.LabelStyle sectionLabel = new Label.LabelStyle();
        sectionLabel.font = font;
        sectionLabel.fontColor = com.badlogic.gdx.graphics.Color.CYAN;
        skin.add("section", sectionLabel);

        Label.LabelStyle statusLabel = new Label.LabelStyle();
        statusLabel.font = font;
        statusLabel.fontColor = com.badlogic.gdx.graphics.Color.LIGHT_GRAY;
        skin.add("status", statusLabel);

        // TextField style
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        textFieldStyle.background = darkDrawable;
        textFieldStyle.cursor = lightDrawable;
        skin.add("default", textFieldStyle);

        // Button styles
        TextButton.TextButtonStyle defaultButton = new TextButton.TextButtonStyle();
        defaultButton.font = font;
        defaultButton.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        defaultButton.up = darkDrawable;
        defaultButton.down = lightDrawable;
        skin.add("default", defaultButton);

        TextButton.TextButtonStyle successButton = new TextButton.TextButtonStyle();
        successButton.font = font;
        successButton.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        successButton.up = greenDrawable;
        successButton.down = lightDrawable;
        skin.add("success", successButton);

        TextButton.TextButtonStyle dangerButton = new TextButton.TextButtonStyle();
        dangerButton.font = font;
        dangerButton.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        dangerButton.up = redDrawable;
        dangerButton.down = lightDrawable;
        skin.add("danger", dangerButton);

        TextButton.TextButtonStyle primaryButton = new TextButton.TextButtonStyle();
        primaryButton.font = font;
        primaryButton.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        primaryButton.up = blueDrawable;
        primaryButton.down = lightDrawable;
        skin.add("primary", primaryButton);

        TextButton.TextButtonStyle warningButton = new TextButton.TextButtonStyle();
        warningButton.font = font;
        warningButton.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        warningButton.up = orangeDrawable;
        warningButton.down = lightDrawable;
        skin.add("warning", warningButton);

        // CheckBox style
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        skin.add("default", checkBoxStyle);

        // SelectBox style
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = com.badlogic.gdx.graphics.Color.WHITE;
        selectBoxStyle.background = darkDrawable;
        selectBoxStyle.scrollStyle = new ScrollPane.ScrollPaneStyle();
        selectBoxStyle.scrollStyle.background = darkDrawable;
        selectBoxStyle.scrollStyle.vScroll = darkDrawable;
        selectBoxStyle.scrollStyle.vScrollKnob = lightDrawable;

        List.ListStyle listStyle = new List.ListStyle();
        listStyle.font = font;
        listStyle.fontColorSelected = com.badlogic.gdx.graphics.Color.WHITE;
        listStyle.fontColorUnselected = com.badlogic.gdx.graphics.Color.LIGHT_GRAY;
        listStyle.selection = blueDrawable;
        listStyle.background = darkDrawable;
        selectBoxStyle.listStyle = listStyle;
        skin.add("default", selectBoxStyle);

        // Window style
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = com.badlogic.gdx.graphics.Color.GOLD;
        windowStyle.background = darkDrawable;
        skin.add("default", windowStyle);

        // ScrollPane style
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = darkDrawable;
        scrollStyle.vScroll = darkDrawable;
        scrollStyle.vScrollKnob = lightDrawable;
        skin.add("default", scrollStyle);
    }

    @Override
    public void render(float delta) {
        if (!initialized || loadingMap) return;

        renderer.render();
        uiStage.act(delta);
        uiStage.draw();
    }

    @Override
    public void resize(int width, int height) {
        if (editorCamera != null) editorCamera.resize(width, height);
        if (uiStage != null) uiStage.getViewport().update(width, height, true);
        if (editorWindow != null) editorWindow.resize(width, height);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (uiStage != null) uiStage.dispose();
        if (skin != null) skin.dispose();
        logger.info("Map Editor disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}