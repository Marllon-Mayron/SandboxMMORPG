// File: MapEditorScreen.java - VERSÃO COMPLETA CORRIGIDA
package com.sandbox.client.editor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.common.sandbox.model.MapJSON;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.core.*;
import com.sandbox.client.editor.models.*;
import com.sandbox.client.editor.ui.FixedLayoutUI;
import com.sandbox.client.editor.utils.MapExporter;
import com.common.sandbox.network.packets.MapLoadResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MapEditorScreen implements Screen, IEditorScreen {
    private static final Logger logger = LoggerFactory.getLogger(MapEditorScreen.class);

    private final SandboxClient game;
    private Stage uiStage;
    private Skin skin;
    private EditorState state;
    private EditorCamera editorCamera;
    private EditorRenderer renderer;
    private EditorInputHandler inputHandler;
    private MapExporter mapExporter;
    private FixedLayoutUI fixedUI;

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

        // Criar skin PRIMEIRO
        createSkin();

        // Depois criar UI fixa (agora o skin já tem todos os drawables)
        fixedUI = new FixedLayoutUI(uiStage, skin, state, editorCamera, game);

        renderer = new EditorRenderer(editorCamera, this, state);
        inputHandler = new EditorInputHandler(editorCamera, this, renderer, state);

        InputMultiplexer multiplexer = new InputMultiplexer(uiStage, inputHandler);
        Gdx.input.setInputProcessor(multiplexer);

        setupCallbacks();
        loadMapFromServer();
    }

    private void setupCallbacks() {
        game.getNetworkClient().setMapSaveCallback(response -> {
            Gdx.app.postRunnable(() -> {
                if (response.success) {
                    logger.info("Map saved successfully");
                    showNotification("Map saved successfully!", Color.GREEN);
                } else {
                    logger.error("Map save failed: {}", response.message);
                    showNotification("Map save failed: " + response.message, Color.RED);
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

                Set<String> requiredSpritesheets = loadChunksAndCollectSpritesheets(response.mapJson);
                loadRequiredSpritesheets(requiredSpritesheets);
                loadChunksIntoEditor(response.mapJson);
            } else {
                logger.error("Failed to load map from server: {}", response.message);
                state.createChunk(0, 0);
                state.setCurrentChunk(0, 0);
            }

            if (fixedUI != null) {
                fixedUI.refreshAll();
            }

            editorCamera.centerOnChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
            editorCamera.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

            loadingMap = false;
            initialized = true;
            logger.info("Map Editor ready");
            showNotification("Map Editor Ready!", Color.GREEN);
        });
    }

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
        return spritesheetPaths;
    }

    private void loadRequiredSpritesheets(Set<String> requiredPaths) {
        logger.info("Loading required spritesheets...");
        state.getSpritesheets().clear();

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
                    String name = file.nameWithoutExtension();
                    SpritesheetData sheet = new SpritesheetData(name, path, texture);
                    state.getSpritesheets().put(path, sheet);
                    logger.info("Loaded spritesheet: {}", path);
                } catch (Exception e) {
                    logger.error("Failed to load spritesheet: {}", path, e);
                }
            } else {
                logger.error("Spritesheet not found: {}", path);
            }
        }

        if (!state.getSpritesheets().isEmpty()) {
            SpritesheetData first = state.getSpritesheets().values().iterator().next();
            state.setCurrentSpritesheet(first);
        }

        if (fixedUI != null) {
            fixedUI.refreshAll();
        }
    }

    private void loadChunksIntoEditor(MapJSON mapJson) {
        state.getChunks().clear();

        for (Map.Entry<String, MapJSON.ChunkData> entry : mapJson.getChunks().entrySet()) {
            String[] coords = entry.getKey().split(":");
            int chunkX = Integer.parseInt(coords[0]);
            int chunkY = Integer.parseInt(coords[1]);

            MapJSON.ChunkData jsonChunk = entry.getValue();
            state.createChunk(chunkX, chunkY);
            ChunkData editorChunk = state.getChunks().get(chunkX + ":" + chunkY);

            if (editorChunk != null) {
                for (int layer = 0; layer < 3; layer++) {
                    for (int x = 0; x < 32; x++) {
                        for (int y = 0; y < 32; y++) {
                            MapJSON.TileData tileData = jsonChunk.getTile(layer, x, y);
                            if (tileData != null && !tileData.isEmpty() && tileData.getTileId() >= 0) {
                                String spritesheetPath = tileData.getSpritesheetPath();
                                int tileId = tileData.getTileId();
                                if (state.getSpritesheets().containsKey(spritesheetPath)) {
                                    editorChunk.setTile(LayerType.fromId(layer), x, y, spritesheetPath, tileId);
                                    logger.debug("Loaded tile layer={} ({},{}): path={}, id={}, tag={}",
                                            layer, x, y, spritesheetPath, tileId, tileData.getTag());
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.getChunks().isEmpty()) {
            state.createChunk(0, 0);
            state.setCurrentChunk(0, 0);
        } else {
            ChunkData firstChunk = state.getChunks().values().iterator().next();
            state.setCurrentChunk(firstChunk.getX(), firstChunk.getY());
        }
    }

    @Override
    public void saveMap() {
        logger.info("Saving map...");
        mapExporter.saveMap(state);
    }

    @Override
    public void refreshUI() {
        if (fixedUI != null) {
            fixedUI.refreshAll();
        }
    }

    @Override
    public boolean isMouseOverUI(int screenX, int screenY) {
        if (fixedUI != null && fixedUI.getStage() != null) {
            return fixedUI.getStage().hit(screenX, screenY, true) != null;
        }
        return false;
    }

    private void showNotification(String message, Color color) {
        logger.info(message);
        // Opcional: implementar um popup temporário
    }

    private void createSkin() {
        skin = new Skin();

        // Criar fonte
        com.badlogic.gdx.graphics.g2d.BitmapFont font = new com.badlogic.gdx.graphics.g2d.BitmapFont();
        font.getData().setScale(0.9f);

        // Definir cores
        Color darkColor = new Color(0.12f, 0.12f, 0.16f, 1);
        Color lightColor = new Color(0.22f, 0.22f, 0.28f, 1);
        Color greenColor = new Color(0.2f, 0.6f, 0.2f, 1);
        Color redColor = new Color(0.7f, 0.2f, 0.2f, 1);
        Color blueColor = new Color(0.2f, 0.4f, 0.7f, 1);
        Color orangeColor = new Color(0.8f, 0.5f, 0.2f, 1);
        Color yellowColor = new Color(0.9f, 0.8f, 0.2f, 1);

        // Criar drawables
        Drawable darkDrawable = createColorDrawable(darkColor);
        Drawable lightDrawable = createColorDrawable(lightColor);
        Drawable greenDrawable = createColorDrawable(greenColor);
        Drawable redDrawable = createColorDrawable(redColor);
        Drawable blueDrawable = createColorDrawable(blueColor);
        Drawable orangeDrawable = createColorDrawable(orangeColor);
        Drawable yellowDrawable = createColorDrawable(yellowColor);

        // Registrar drawables
        skin.add("window-bg", darkDrawable);
        skin.add("white", darkDrawable);
        skin.add("green", greenDrawable);
        skin.add("red", redDrawable);
        skin.add("blue", blueDrawable);
        skin.add("orange", orangeDrawable);
        skin.add("yellow", yellowDrawable);

        // Estilos de label
        Label.LabelStyle defaultLabel = new Label.LabelStyle();
        defaultLabel.font = font;
        defaultLabel.fontColor = Color.WHITE;
        skin.add("default", defaultLabel);

        Label.LabelStyle sectionLabel = new Label.LabelStyle();
        sectionLabel.font = font;
        sectionLabel.fontColor = Color.CYAN;
        skin.add("section", sectionLabel);

        Label.LabelStyle statusLabel = new Label.LabelStyle();
        statusLabel.font = font;
        statusLabel.fontColor = Color.LIGHT_GRAY;
        skin.add("status", statusLabel);

        // Estilos de botão
        TextButton.TextButtonStyle defaultButton = new TextButton.TextButtonStyle();
        defaultButton.font = font;
        defaultButton.fontColor = Color.WHITE;
        defaultButton.up = darkDrawable;
        defaultButton.down = lightDrawable;
        defaultButton.over = lightDrawable;
        skin.add("default", defaultButton);

        TextButton.TextButtonStyle successButton = new TextButton.TextButtonStyle();
        successButton.font = font;
        successButton.fontColor = Color.WHITE;
        successButton.up = greenDrawable;
        successButton.down = lightDrawable;
        successButton.over = lightDrawable;
        skin.add("success", successButton);

        TextButton.TextButtonStyle dangerButton = new TextButton.TextButtonStyle();
        dangerButton.font = font;
        dangerButton.fontColor = Color.WHITE;
        dangerButton.up = redDrawable;
        dangerButton.down = lightDrawable;
        dangerButton.over = lightDrawable;
        skin.add("danger", dangerButton);

        TextButton.TextButtonStyle primaryButton = new TextButton.TextButtonStyle();
        primaryButton.font = font;
        primaryButton.fontColor = Color.WHITE;
        primaryButton.up = blueDrawable;
        primaryButton.down = lightDrawable;
        primaryButton.over = lightDrawable;
        skin.add("primary", primaryButton);

        TextButton.TextButtonStyle warningButton = new TextButton.TextButtonStyle();
        warningButton.font = font;
        warningButton.fontColor = Color.WHITE;
        warningButton.up = orangeDrawable;
        warningButton.down = lightDrawable;
        warningButton.over = lightDrawable;
        skin.add("warning", warningButton);

        // Estilo de texto
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.WHITE;
        textFieldStyle.background = darkDrawable;
        textFieldStyle.cursor = lightDrawable;
        textFieldStyle.selection = blueDrawable;
        skin.add("default", textFieldStyle);

        // Estilo de checkbox
        CheckBox.CheckBoxStyle checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.WHITE;
        checkBoxStyle.checkboxOn = blueDrawable;
        checkBoxStyle.checkboxOff = darkDrawable;
        skin.add("default", checkBoxStyle);

        // Estilo de janela
        Window.WindowStyle windowStyle = new Window.WindowStyle();
        windowStyle.titleFont = font;
        windowStyle.titleFontColor = Color.GOLD;
        windowStyle.background = darkDrawable;
        skin.add("default", windowStyle);

        // Estilo de scroll
        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle();
        scrollStyle.background = darkDrawable;
        scrollStyle.vScroll = darkDrawable;
        scrollStyle.vScrollKnob = lightDrawable;
        scrollStyle.hScroll = darkDrawable;
        scrollStyle.hScrollKnob = lightDrawable;
        skin.add("default", scrollStyle);

        // Estilo de select box
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle();
        selectBoxStyle.font = font;
        selectBoxStyle.fontColor = Color.WHITE;
        selectBoxStyle.background = darkDrawable;
        selectBoxStyle.scrollStyle = scrollStyle;
        selectBoxStyle.listStyle = new List.ListStyle();
        selectBoxStyle.listStyle.font = font;
        selectBoxStyle.listStyle.fontColorSelected = Color.WHITE;
        selectBoxStyle.listStyle.fontColorUnselected = Color.LIGHT_GRAY;
        selectBoxStyle.listStyle.selection = blueDrawable;
        selectBoxStyle.listStyle.background = darkDrawable;
        skin.add("default", selectBoxStyle);
    }

    private Drawable createColorDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(texture);
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
        if (fixedUI != null) fixedUI.resize(width, height);
    }

    @Override
    public void dispose() {
        if (renderer != null) renderer.dispose();
        if (fixedUI != null) fixedUI.dispose();
        if (skin != null) skin.dispose();
        logger.info("Map Editor disposed");
    }

    @Override public void pause() {}
    @Override public void resume() {}
    @Override public void hide() {}
}