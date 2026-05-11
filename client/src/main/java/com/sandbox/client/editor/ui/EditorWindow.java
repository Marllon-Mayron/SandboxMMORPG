package com.sandbox.client.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.core.EditorCamera;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.utils.MapExporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorWindow {
    private static final Logger logger = LoggerFactory.getLogger(EditorWindow.class);

    private Window window;
    private Skin skin;
    private SandboxClient game;
    private EditorCamera editorCamera;
    private EditorState state;
    private MapExporter mapExporter;

    private ChunkPanel chunkPanel;
    private LayerPanel layerPanel;
    private SpritesheetPanel spritesheetPanel;
    private TilePalette tilePalette;
    private TagEditorPanel tagEditorPanel;

    public EditorWindow(Skin skin, SandboxClient game, EditorCamera editorCamera, EditorState state, MapExporter mapExporter) {
        this.skin = skin;
        this.game = game;
        this.editorCamera = editorCamera;
        this.state = state;
        this.mapExporter = mapExporter;

        createWindow();
    }

    private void createWindow() {
        window = new Window("MAP EDITOR", skin);
        window.setPosition(10, 10);
        window.setWidth(360);
        window.setMovable(true);
        window.setResizable(true);
        window.setModal(false);

        // Garantir que a window pode receber toque/clique
        window.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        Table content = new Table();
        content.pad(10);
        content.setTouchable(com.badlogic.gdx.scenes.scene2d.Touchable.enabled);

        // ===== LAYER PANEL =====
        Label layerSectionLabel = new Label("CURRENT LAYER", skin, "section");
        layerSectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(layerSectionLabel).left().padBottom(5);
        content.row();

        layerPanel = new LayerPanel(skin, state);
        content.add(layerPanel.getTable()).width(340).padBottom(10);
        content.row();

        // ===== SELECTED BRUSH =====
        Label brushLabel = new Label("SELECTED BRUSH", skin, "section");
        brushLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(brushLabel).left().padBottom(5);
        content.row();

        Label selectedSpriteLabel = new Label("None selected", skin);
        selectedSpriteLabel.setColor(com.badlogic.gdx.graphics.Color.GOLD);
        content.add(selectedSpriteLabel).left().padBottom(10);
        content.row();

        // ===== SPRITESHEET PANEL =====
        Label spritesheetSectionLabel = new Label("SPRITESHEETS", skin, "section");
        spritesheetSectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(spritesheetSectionLabel).left().padBottom(5);
        content.row();

        spritesheetPanel = new SpritesheetPanel(skin, game, state, selectedSpriteLabel);
        content.add(spritesheetPanel.getTable()).width(340).padBottom(10);
        content.row();

        // ===== TILE PALETTE =====
        Label paletteLabel = new Label("TILE PALETTE", skin, "section");
        paletteLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(paletteLabel).left().padBottom(5);
        content.row();

        tilePalette = new TilePalette(skin, state, selectedSpriteLabel);
        content.add(tilePalette.getScrollPane()).width(340).height(180).padBottom(10);
        content.row();

        // ===== TAG EDITOR =====
        Label tagSectionLabel = new Label("TILE TAGS", skin, "section");
        tagSectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(tagSectionLabel).left().padBottom(5);
        content.row();

        tagEditorPanel = new TagEditorPanel(skin, state);
        content.add(tagEditorPanel.getTable()).width(340).padBottom(10);
        content.row();

        // ===== CHUNK PANEL =====
        Label chunkSectionLabel = new Label("CHUNKS", skin, "section");
        chunkSectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(chunkSectionLabel).left().padBottom(5);
        content.row();

        chunkPanel = new ChunkPanel(skin, game, state, editorCamera);
        content.add(chunkPanel.getTable()).width(340).padBottom(10);
        content.row();

        // ===== DISPLAY OPTIONS =====
        Label optionsLabel = new Label("DISPLAY OPTIONS", skin, "section");
        optionsLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(optionsLabel).left().padBottom(5);
        content.row();

        CheckBox gridCheck = new CheckBox("Show Grid (selected chunk only)", skin);
        gridCheck.setChecked(true);
        gridCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setShowGrid(gridCheck.isChecked());
                logger.debug("Show grid: {}", state.isShowGrid());
            }
        });
        content.add(gridCheck).left().padBottom(5);
        content.row();

        CheckBox boundsCheck = new CheckBox("Show All Chunk Bounds", skin);
        boundsCheck.setChecked(true);
        boundsCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setShowChunkBounds(boundsCheck.isChecked());
                logger.debug("Show chunk bounds: {}", state.isShowChunkBounds());
            }
        });
        content.add(boundsCheck).left().padBottom(15);
        content.row();

        // ===== SAVE BUTTON =====
        TextButton saveButton = new TextButton("SAVE MAP TO SERVER", skin, "success");
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                logger.info("Save button clicked!");
                saveMap();
            }
        });
        content.add(saveButton).width(340).padBottom(10);
        content.row();

        // ===== INFO SECTION =====
        Label infoLabel = new Label("INFO", skin, "section");
        infoLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        content.add(infoLabel).left().padBottom(5);
        content.row();

        Label chunkLabel = new Label("Chunk: [0,0]", skin);
        content.add(chunkLabel).left().padBottom(3);
        content.row();

        Label coordLabel = new Label("Tile: ---", skin);
        content.add(coordLabel).left().padBottom(3);
        content.row();

        Label statusLabel = new Label("Ready", skin);
        content.add(statusLabel).left().padTop(10).padBottom(5);

        window.add(content).fill().expand();

        logger.info("EditorWindow created successfully");
    }

    public void saveMap() {
        logger.info("Saving map...");
        mapExporter.saveMap(state);
    }

    public void updateLayerDisplay() {
        if (layerPanel != null) {
            layerPanel.updateCurrentLayer();
        }
    }

    public void refreshUI() {
        logger.debug("Refreshing UI");
        if (spritesheetPanel != null) {
            spritesheetPanel.refresh();
        }
        if (tilePalette != null) {
            tilePalette.refresh();
        }
        if (chunkPanel != null) {
            chunkPanel.refresh();
        }
    }

    public void refreshTilePalette() {
        if (tilePalette != null) {
            tilePalette.refresh();
            logger.info("TilePalette refreshed");
        }
    }

    public void updateChunkDisplay(int x, int y) {
        if (chunkPanel != null) {
            chunkPanel.updateChunkDisplay(x, y);
        }
    }

    public boolean isMouseOver(int screenX, int screenY) {
        Vector2 localCoords = window.stageToLocalCoordinates(new Vector2(screenX, screenY));
        Actor hit = window.hit(localCoords.x, localCoords.y, true);
        boolean over = hit != null;
        if (over) {
            logger.debug("Mouse over UI at local coords: {}, {}", localCoords.x, localCoords.y);
        }
        return over;
    }

    public void resize(int width, int height) {
        window.setPosition(10, 10);
        window.setHeight(height - 20);
    }

    public Window getWindow() {
        return window;
    }
}