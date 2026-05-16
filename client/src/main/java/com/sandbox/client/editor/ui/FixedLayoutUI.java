package com.sandbox.client.editor.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.common.sandbox.model.world.TileTag;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.core.EditorCamera;
import com.sandbox.client.editor.models.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;

public class FixedLayoutUI {
    private static final Logger logger = LoggerFactory.getLogger(FixedLayoutUI.class);

    private Stage stage;
    private Skin skin;
    private EditorState state;
    private EditorCamera camera;
    private SandboxClient game;

    // Componentes UI
    private Window leftPanel;
    private Window rightPanel;
    private Window topPanel;
    private Label selectedBrushLabel;
    private Label currentLayerLabel;
    private Label currentChunkLabel;
    private Label currentTagLabel;
    private Table chunkListTable;
    private Table spritesheetListTable;
    private Table tilePaletteTable;

    // Componentes de Tags
    private TextField tagNameField;
    private SelectBox<String> tagPresetSelect;

    // Dimensões fixas
    private static final int LEFT_PANEL_WIDTH = 300;
    private static final int RIGHT_PANEL_WIDTH = 340;
    private static final int TOP_PANEL_HEIGHT = 45;
    private static final int PANEL_MARGIN = 5;

    public FixedLayoutUI(Stage stage, Skin skin, EditorState state, EditorCamera camera, SandboxClient game) {
        this.stage = stage;
        this.skin = skin;
        this.state = state;
        this.camera = camera;
        this.game = game;
        this.selectedBrushLabel = new Label("No tile selected", skin);
        this.selectedBrushLabel.setColor(Color.GOLD);
        this.currentTagLabel = new Label("Tag: none", skin);
        this.currentTagLabel.setColor(Color.LIGHT_GRAY);
        this.chunkListTable = new Table();
        this.spritesheetListTable = new Table();
        this.tilePaletteTable = new Table();

        createLayout();
    }

    public void createLayout() {
        stage.getActors().clear();
        createTopPanel();
        createLeftPanel();
        createRightPanel();
        refreshAll();
    }

    private void createTopPanel() {
        topPanel = new Window("", skin);
        topPanel.setMovable(false);
        topPanel.setResizable(false);
        topPanel.setWidth(Gdx.graphics.getWidth());
        topPanel.setHeight(TOP_PANEL_HEIGHT);
        topPanel.setPosition(0, Gdx.graphics.getHeight() - TOP_PANEL_HEIGHT);

        if (skin.has("window-bg", Drawable.class)) {
            topPanel.setBackground(skin.getDrawable("window-bg"));
        }

        Table menuTable = new Table();
        menuTable.pad(5);
        menuTable.defaults().padRight(10);

        // Titulo do editor
        Label titleLabel = new Label("MAP EDITOR", skin, "section");
        titleLabel.setColor(Color.ORANGE);
        menuTable.add(titleLabel).padRight(20);

        // Botao Salvar
        TextButton saveBtn = new TextButton("Save (Ctrl+S)", skin, "success");
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (game.getScreen() instanceof com.sandbox.client.editor.MapEditorScreen) {
                    ((com.sandbox.client.editor.MapEditorScreen) game.getScreen()).saveMap();
                }
            }
        });
        menuTable.add(saveBtn);

        // Botao Reset Layout
        TextButton resetBtn = new TextButton("Reset Layout", skin, "default");
        resetBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                createLayout();
            }
        });
        menuTable.add(resetBtn);

        // Espacador
        menuTable.add().expandX();

        // Info do brush selecionado
        Table brushInfo = new Table();
        brushInfo.add(new Label("Brush: ", skin)).padRight(5);
        brushInfo.add(selectedBrushLabel);
        menuTable.add(brushInfo);

        topPanel.add(menuTable).fill().expand();
        stage.addActor(topPanel);
    }

    private void createLeftPanel() {
        leftPanel = new Window("CONTROLS", skin);
        leftPanel.setMovable(false);
        leftPanel.setResizable(false);
        leftPanel.setWidth(LEFT_PANEL_WIDTH);
        leftPanel.setHeight(Gdx.graphics.getHeight() - TOP_PANEL_HEIGHT - PANEL_MARGIN * 2);
        leftPanel.setPosition(PANEL_MARGIN, PANEL_MARGIN);

        if (skin.has("window-bg", Drawable.class)) {
            leftPanel.setBackground(skin.getDrawable("window-bg"));
        }

        Table mainTable = new Table();
        mainTable.pad(10);
        mainTable.defaults().fillX().padBottom(12);

        // ========== TOOLS SECTION ==========
        addSectionHeader(mainTable, "TOOLS", Color.CYAN);
        mainTable.add(createToolsSection()).padBottom(15);
        mainTable.row();

        // ========== LAYERS SECTION ==========
        addSectionHeader(mainTable, "LAYERS", Color.CYAN);
        mainTable.add(createLayersSection()).padBottom(15);
        mainTable.row();

        // ========== CHUNKS SECTION ==========
        addSectionHeader(mainTable, "CHUNKS", Color.CYAN);
        mainTable.add(createChunksSection()).padBottom(15);
        mainTable.row();

        // ========== DISPLAY SECTION ==========
        addSectionHeader(mainTable, "DISPLAY", Color.CYAN);
        mainTable.add(createDisplaySection());

        ScrollPane scrollPane = new ScrollPane(mainTable, skin);
        scrollPane.setScrollingDisabled(false, true);
        scrollPane.setFadeScrollBars(false);
        leftPanel.add(scrollPane).fill().expand();
        stage.addActor(leftPanel);
    }

    private void addSectionHeader(Table table, String title, Color color) {
        Label header = new Label(title, skin, "section");
        header.setColor(color);
        table.add(header).left().padBottom(5);
        table.row();
    }

    private Table createToolsSection() {
        Table table = new Table();
        table.defaults().padRight(8);

        TextButton brushBtn = new TextButton("BRUSH", skin,
                state.getCurrentTool() == ToolType.BRUSH ? "warning" : "default");
        brushBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setCurrentTool(ToolType.BRUSH);
                updateToolButtons(brushBtn, null);
            }
        });

        TextButton bucketBtn = new TextButton("BUCKET", skin,
                state.getCurrentTool() == ToolType.BUCKET ? "warning" : "default");
        bucketBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setCurrentTool(ToolType.BUCKET);
                updateToolButtons(null, bucketBtn);
            }
        });

        table.add(brushBtn).width(130);
        table.add(bucketBtn).width(130);

        // Hotkeys info
        table.row();
        Label hotkeyInfo = new Label("[ / ] to switch tools", skin, "status");
        hotkeyInfo.setFontScale(0.8f);
        table.add(hotkeyInfo).colspan(2).left().padTop(5);

        return table;
    }

    private void updateToolButtons(TextButton brushBtn, TextButton bucketBtn) {
        if (brushBtn != null) {
            String style = state.getCurrentTool() == ToolType.BRUSH ? "warning" : "default";
            brushBtn.setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
        if (bucketBtn != null) {
            String style = state.getCurrentTool() == ToolType.BUCKET ? "warning" : "default";
            bucketBtn.setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
    }

    private Table createLayersSection() {
        Table table = new Table();
        table.defaults().padRight(6).padBottom(5);

        for (LayerType layer : LayerType.values()) {
            TextButton btn = new TextButton(layer.name, skin,
                    state.getCurrentLayer() == layer ? "warning" : "default");
            final LayerType finalLayer = layer;
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentLayer(finalLayer);
                    updateLayerButtons();
                    if (currentLayerLabel != null) {
                        currentLayerLabel.setText("Current: " + state.getCurrentLayer().name);
                        currentLayerLabel.setColor(state.getCurrentLayer().color);
                    }
                }
            });
            table.add(btn).width(85);
        }

        table.row();
        currentLayerLabel = new Label("Current: " + state.getCurrentLayer().name, skin);
        currentLayerLabel.setColor(state.getCurrentLayer().color);
        table.add(currentLayerLabel).colspan(3).left().padTop(5);

        // Hotkeys info
        table.row();
        Label hotkeyInfo = new Label("Hotkeys: 1=Ground | 2=Decoration | 3=Ceiling", skin, "status");
        hotkeyInfo.setFontScale(0.7f);
        table.add(hotkeyInfo).colspan(3).left().padTop(5);

        return table;
    }

    private void updateLayerButtons() {
        Table parent = (Table) leftPanel.getChildren().peek();
        if (parent instanceof Table) {
            parent.clearChildren();
            parent.pad(10);
            parent.defaults().fillX().padBottom(12);

            addSectionHeader(parent, "TOOLS", Color.CYAN);
            parent.add(createToolsSection()).padBottom(15);
            parent.row();

            addSectionHeader(parent, "LAYERS", Color.CYAN);
            parent.add(createLayersSection()).padBottom(15);
            parent.row();

            addSectionHeader(parent, "CHUNKS", Color.CYAN);
            parent.add(createChunksSection()).padBottom(15);
            parent.row();

            addSectionHeader(parent, "DISPLAY", Color.CYAN);
            parent.add(createDisplaySection());

            parent.invalidateHierarchy();
        }
    }

    private Table createChunksSection() {
        Table table = new Table();
        table.defaults().padBottom(6);

        // Current chunk display
        currentChunkLabel = new Label("Current: [" + state.getCurrentChunkX() + "," + state.getCurrentChunkY() + "]", skin);
        currentChunkLabel.setColor(Color.GOLD);
        table.add(currentChunkLabel).left();
        table.row();

        // Navigation
        Table navTable = new Table();
        TextField xField = new TextField(String.valueOf(state.getCurrentChunkX()), skin);
        xField.setWidth(60);
        TextField yField = new TextField(String.valueOf(state.getCurrentChunkY()), skin);
        yField.setWidth(60);

        TextButton goBtn = new TextButton("Go", skin, "primary");
        goBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int x = Integer.parseInt(xField.getText());
                    int y = Integer.parseInt(yField.getText());
                    String key = x + ":" + y;
                    if (state.getChunks().containsKey(key)) {
                        state.setCurrentChunk(x, y);
                        camera.centerOnChunk(x, y);
                        if (currentChunkLabel != null) {
                            currentChunkLabel.setText("Current: [" + x + "," + y + "]");
                        }
                        refreshChunkList();
                    } else {
                        showNotification("Chunk [" + x + "," + y + "] not found!");
                    }
                } catch (NumberFormatException e) {
                    showNotification("Invalid coordinates!");
                }
            }
        });

        navTable.add(new Label("X:", skin));
        navTable.add(xField).width(60).padRight(10);
        navTable.add(new Label("Y:", skin));
        navTable.add(yField).width(60).padRight(10);
        navTable.add(goBtn);
        table.add(navTable).left();
        table.row();

        // Create new chunk
        Table createTable = new Table();
        TextField newXField = new TextField("0", skin);
        newXField.setWidth(60);
        TextField newYField = new TextField("0", skin);
        newYField.setWidth(60);

        TextButton createBtn = new TextButton("Create", skin, "success");
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int x = Integer.parseInt(newXField.getText());
                    int y = Integer.parseInt(newYField.getText());
                    state.createChunk(x, y);
                    state.setCurrentChunk(x, y);
                    camera.centerOnChunk(x, y);
                    if (currentChunkLabel != null) {
                        currentChunkLabel.setText("Current: [" + x + "," + y + "]");
                    }
                    refreshChunkList();
                    showNotification("Chunk [" + x + "," + y + "] created!");
                } catch (NumberFormatException e) {
                    showNotification("Invalid coordinates!");
                }
            }
        });

        TextButton deleteBtn = new TextButton("Delete Current", skin, "danger");
        deleteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (state.getChunks().size() <= 1) {
                    showNotification("Cannot delete the only chunk!");
                    return;
                }
                int oldX = state.getCurrentChunkX();
                int oldY = state.getCurrentChunkY();
                state.deleteChunk(oldX, oldY);
                camera.centerOnChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
                if (currentChunkLabel != null) {
                    currentChunkLabel.setText("Current: [" + state.getCurrentChunkX() + "," + state.getCurrentChunkY() + "]");
                }
                refreshChunkList();
                showNotification("Chunk [" + oldX + "," + oldY + "] deleted!");
            }
        });

        createTable.add(new Label("New:", skin));
        createTable.add(newXField).width(60).padRight(10);
        createTable.add(new Label("Y:", skin));
        createTable.add(newYField).width(60).padRight(10);
        createTable.add(createBtn);
        table.add(createTable).left();
        table.row();
        table.add(deleteBtn).left().padTop(5);
        table.row();

        // Chunk list
        Label listLabel = new Label("Existing Chunks:", skin, "status");
        listLabel.setColor(Color.LIGHT_GRAY);
        table.add(listLabel).left().padTop(10);
        table.row();

        refreshChunkListTable();
        ScrollPane chunkScroll = new ScrollPane(chunkListTable, skin);
        chunkScroll.setHeight(140);
        chunkScroll.setFadeScrollBars(false);
        table.add(chunkScroll).width(270).height(140);

        return table;
    }

    private void refreshChunkListTable() {
        chunkListTable.clear();
        chunkListTable.defaults().padRight(5).padBottom(5);

        int colCount = 0;
        for (ChunkData chunk : state.getChunks().values()) {
            boolean isCurrent = (chunk.getX() == state.getCurrentChunkX() && chunk.getY() == state.getCurrentChunkY());
            TextButton chunkBtn = new TextButton("[" + chunk.getX() + "," + chunk.getY() + "]", skin,
                    isCurrent ? "warning" : "default");
            chunkBtn.getLabel().setFontScale(0.8f);
            final int fx = chunk.getX(), fy = chunk.getY();
            chunkBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentChunk(fx, fy);
                    camera.centerOnChunk(fx, fy);
                    if (currentChunkLabel != null) {
                        currentChunkLabel.setText("Current: [" + fx + "," + fy + "]");
                    }
                    refreshChunkList();
                }
            });
            chunkListTable.add(chunkBtn).width(70);
            colCount++;
            if (colCount % 3 == 0) {
                chunkListTable.row();
            }
        }
    }

    private void refreshChunkList() {
        refreshChunkListTable();
        if (currentChunkLabel != null) {
            currentChunkLabel.setText("Current: [" + state.getCurrentChunkX() + "," + state.getCurrentChunkY() + "]");
        }
    }

    private Table createDisplaySection() {
        Table table = new Table();
        table.defaults().left().padBottom(8);

        CheckBox gridCheck = new CheckBox("Show Grid (H)", skin);
        gridCheck.setChecked(state.isShowGrid());
        gridCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setShowGrid(gridCheck.isChecked());
            }
        });
        table.add(gridCheck);
        table.row();

        CheckBox boundsCheck = new CheckBox("Show Chunk Bounds (B)", skin);
        boundsCheck.setChecked(state.isShowChunkBounds());
        boundsCheck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.setShowChunkBounds(boundsCheck.isChecked());
            }
        });
        table.add(boundsCheck);

        return table;
    }

    private void createRightPanel() {
        rightPanel = new Window("ASSETS & TAGS", skin);
        rightPanel.setMovable(false);
        rightPanel.setResizable(false);
        rightPanel.setWidth(RIGHT_PANEL_WIDTH);
        rightPanel.setHeight(Gdx.graphics.getHeight() - TOP_PANEL_HEIGHT - PANEL_MARGIN * 2);
        rightPanel.setPosition(Gdx.graphics.getWidth() - RIGHT_PANEL_WIDTH - PANEL_MARGIN, PANEL_MARGIN);

        if (skin.has("window-bg", Drawable.class)) {
            rightPanel.setBackground(skin.getDrawable("window-bg"));
        }

        Table mainTable = new Table();
        mainTable.pad(10);
        mainTable.defaults().fillX().padBottom(12);

        // ========== SPRITESHEETS SECTION ==========
        addSectionHeader(mainTable, "SPRITESHEETS", Color.CYAN);
        mainTable.add(createSpritesheetSection()).padBottom(15);
        mainTable.row();

        // ========== TILE PALETTE SECTION ==========
        addSectionHeader(mainTable, "TILE PALETTE", Color.CYAN);
        mainTable.add(createTilePaletteSection()).padBottom(15);
        mainTable.row();

        // ========== TAGS SECTION ==========
        addSectionHeader(mainTable, "TILE TAGS", Color.CYAN);
        mainTable.add(createTagsSection()).expand().fill();

        ScrollPane scrollPane = new ScrollPane(mainTable, skin);
        scrollPane.setScrollingDisabled(false, true);
        scrollPane.setFadeScrollBars(false);
        rightPanel.add(scrollPane).fill().expand();
        stage.addActor(rightPanel);
    }

    private Table createSpritesheetSection() {
        Table table = new Table();
        table.defaults().padBottom(8);

        // Load spritesheet controls
        Table loadTable = new Table();
        TextField nameField = new TextField("", skin);
        nameField.setMessageText("name (optional)");

        TextButton loadBtn = new TextButton("Load Image", skin, "primary");
        loadBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadSpritesheet(nameField.getText().trim());
            }
        });

        loadTable.add(nameField).width(140).padRight(8);
        loadTable.add(loadBtn);
        table.add(loadTable).left();
        table.row();

        // Spritesheet list
        refreshSpritesheetList();
        ScrollPane sheetScroll = new ScrollPane(spritesheetListTable, skin);
        sheetScroll.setHeight(120);
        sheetScroll.setFadeScrollBars(false);
        table.add(sheetScroll).width(310).height(120);

        return table;
    }

    private void refreshSpritesheetList() {
        spritesheetListTable.clear();
        spritesheetListTable.defaults().padBottom(4);

        if (state.getSpritesheets().isEmpty()) {
            Label emptyLabel = new Label("No spritesheets loaded", skin, "status");
            spritesheetListTable.add(emptyLabel).left().pad(10);
            return;
        }

        for (SpritesheetData sheet : state.getSpritesheets().values()) {
            Table itemTable = new Table();
            boolean isCurrent = (state.getCurrentSpritesheet() == sheet);

            Label nameLabel = new Label(sheet.getName() + " (" + sheet.getCols() + "x" + sheet.getRows() + ")", skin);
            if (isCurrent) {
                nameLabel.setColor(Color.GREEN);
            }
            itemTable.add(nameLabel).left().expandX();

            TextButton selectBtn = new TextButton("Select", skin, isCurrent ? "warning" : "primary");
            final SpritesheetData finalSheet = sheet;
            selectBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentSpritesheet(finalSheet);
                    refreshTilePalette();
                    refreshSpritesheetList();
                    showNotification("Selected: " + finalSheet.getName());
                }
            });
            itemTable.add(selectBtn).width(60);

            spritesheetListTable.add(itemTable).width(290);
            spritesheetListTable.row();
        }
    }

    private Table createTilePaletteSection() {
        Table table = new Table();
        table.defaults().padBottom(5);

        refreshTilePaletteTable();
        ScrollPane tileScroll = new ScrollPane(tilePaletteTable, skin);
        tileScroll.setFadeScrollBars(false);
        table.add(tileScroll).width(310).height(200);

        return table;
    }

    private void refreshTilePaletteTable() {
        tilePaletteTable.clear();

        if (state.getCurrentSpritesheet() == null) {
            Label emptyLabel = new Label("Select a spritesheet above", skin, "status");
            emptyLabel.setWrap(true);
            tilePaletteTable.add(emptyLabel).center().pad(20).width(280);
            return;
        }

        SpritesheetData sheet = state.getCurrentSpritesheet();
        int cols = 4;
        int totalSprites = sheet.getTotalSprites();

        tilePaletteTable.defaults().pad(2);

        for (int i = 0; i < totalSprites; i++) {
            final int tileId = i;
            final String path = sheet.getPath();

            ImageButton tileBtn = new ImageButton(new TextureRegionDrawable(sheet.getSprite(i)));
            tileBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    TileRef brush = new TileRef(path, tileId);
                    // Preserve existing tag if any
                    if (state.getSelectedBrush() != null && state.getSelectedBrush().getTag() != null) {
                        brush.setTag(state.getSelectedBrush().getTag());
                    }
                    state.setSelectedBrush(brush);
                    selectedBrushLabel.setText(sheet.getName() + " - Tile " + tileId);
                    selectedBrushLabel.setColor(Color.GREEN);
                    updateTagDisplay();
                    showNotification("Selected: " + sheet.getName() + " Tile " + tileId);
                }
            });

            tilePaletteTable.add(tileBtn).size(42, 42);
            if ((i + 1) % cols == 0) {
                tilePaletteTable.row();
            }
        }
    }

    private void refreshTilePalette() {
        refreshTilePaletteTable();
    }

    private Table createTagsSection() {
        Table table = new Table();
        table.defaults().padBottom(8);

        // Current tag display
        updateTagDisplay();
        table.add(currentTagLabel).left().padBottom(5);
        table.row();

        // Tag presets
        Table presetTable = new Table();
        presetTable.add(new Label("Presets:", skin)).padRight(5);

        String[] presets = {"none", "solid", "water", "lava", "grass", "sand", "ice", "mud"};
        tagPresetSelect = new SelectBox<>(skin);
        tagPresetSelect.setItems(presets);
        tagPresetSelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = tagPresetSelect.getSelected();
                if (!"none".equals(selected)) {
                    applyTagPreset(selected);
                }
            }
        });
        presetTable.add(tagPresetSelect).width(120);
        table.add(presetTable).left().padBottom(5);
        table.row();

        // Custom tag
        Table customTable = new Table();
        customTable.add(new Label("Custom:", skin)).padRight(5);
        tagNameField = new TextField("", skin);
        customTable.add(tagNameField).width(120).padRight(10);

        TextButton applyTagBtn = new TextButton("Apply", skin, "primary");
        applyTagBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String tagName = tagNameField.getText().trim();
                if (!tagName.isEmpty()) {
                    applyCustomTag(tagName);
                }
            }
        });
        customTable.add(applyTagBtn).width(60);
        table.add(customTable).left().padBottom(5);
        table.row();

        // Clear tag button
        TextButton clearTagBtn = new TextButton("Clear Tag", skin, "default");
        clearTagBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clearCurrentTileTag();
            }
        });
        table.add(clearTagBtn).width(260).padBottom(5);

        return table;
    }

    private void updateTagDisplay() {
        TileRef brush = state.getSelectedBrush();
        if (brush != null && brush.isValid() && brush.getTag() != null) {
            TileTag tag = brush.getTag();
            String tagInfo = tag.getName();
            if (tag.isSolid()) {
                tagInfo += " (blocks movement)";
            } else if (tag.isWater()) {
                tagInfo += " (slows movement)";
            } else if (tag.isLava()) {
                tagInfo += " (damages player)";
            }
            currentTagLabel.setText("Current tag: " + tagInfo);
            currentTagLabel.setColor(Color.GREEN);
        } else {
            currentTagLabel.setText("Current tag: none");
            currentTagLabel.setColor(Color.LIGHT_GRAY);
        }
    }

    private void applyTagPreset(String preset) {
        TileTag tag;
        switch (preset) {
            case "none":
                tag = new TileTag("none");
                break;
            case "water":
                tag = TileTag.water();
                break;
            case "solid":
                tag = TileTag.solid();
                break;
            case "lava":
                tag = TileTag.lava();
                break;
            case "grass":
                tag = TileTag.grass();
                break;
            case "sand":
                tag = TileTag.sand();
                break;
            case "ice":
                tag = TileTag.ice();
                break;
            case "mud":
                tag = TileTag.mud();
                break;
            default:
                return;
        }
        applyTagToBrush(tag);
        tagPresetSelect.setSelected("none");
    }

    private void applyCustomTag(String tagName) {
        TileTag tag = new TileTag(tagName);
        applyTagToBrush(tag);
        tagNameField.setText("");
    }

    private void applyTagToBrush(TileTag tag) {
        TileRef brush = state.getSelectedBrush();
        if (brush != null && brush.isValid()) {
            brush.setTag(tag);
            updateTagDisplay();
            showNotification("Tag applied: " + tag.getName());
        } else {
            showNotification("Select a tile first!");
        }
    }

    private void clearCurrentTileTag() {
        TileRef brush = state.getSelectedBrush();
        if (brush != null && brush.isValid()) {
            brush.setTag(new TileTag("default"));
            updateTagDisplay();
            showNotification("Tag cleared");
        }
    }

    private void loadSpritesheet(String customName) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Image files", "png", "jpg", "jpeg");
        fileChooser.setFileFilter(filter);

        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            String name = customName.isEmpty() ? file.getName().replaceFirst("[.][^.]+$", "") : customName;

            try {
                Texture texture = new Texture(Gdx.files.absolute(file.getAbsolutePath()));
                texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
                String relativePath = "spritesheets/" + file.getName();
                SpritesheetData sheet = new SpritesheetData(name, relativePath, texture);
                state.getSpritesheets().put(relativePath, sheet);
                state.setCurrentSpritesheet(sheet);
                refreshSpritesheetList();
                refreshTilePalette();
                showNotification("Loaded: " + name);
                logger.info("Loaded spritesheet: {}", name);
            } catch (Exception e) {
                logger.error("Failed to load spritesheet", e);
                showNotification("Failed to load image!");
            }
        }
    }

    private void showNotification(String message) {
        logger.info(message);
        selectedBrushLabel.setText(message);
        selectedBrushLabel.setColor(Color.YELLOW);

        Gdx.app.postRunnable(() -> {
            try {
                Thread.sleep(2000);
                if (state.getSelectedBrush() != null && state.getSelectedBrush().isValid()) {
                    SpritesheetData sheet = state.getCurrentSpritesheet();
                    if (sheet != null) {
                        selectedBrushLabel.setText(sheet.getName() + " - Tile " + state.getSelectedBrush().getTileId());
                    } else {
                        selectedBrushLabel.setText("Tile " + state.getSelectedBrush().getTileId());
                    }
                    selectedBrushLabel.setColor(Color.GREEN);
                    updateTagDisplay();
                } else {
                    selectedBrushLabel.setText("No tile selected");
                    selectedBrushLabel.setColor(Color.GOLD);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    public void refreshAll() {
        refreshChunkList();
        refreshSpritesheetList();
        refreshTilePalette();
        updateTagDisplay();
        if (currentLayerLabel != null) {
            currentLayerLabel.setText("Current: " + state.getCurrentLayer().name);
            currentLayerLabel.setColor(state.getCurrentLayer().color);
        }
        if (currentChunkLabel != null) {
            currentChunkLabel.setText("Current: [" + state.getCurrentChunkX() + "," + state.getCurrentChunkY() + "]");
        }
        if (selectedBrushLabel != null && state.getSelectedBrush() != null && state.getSelectedBrush().isValid()) {
            SpritesheetData sheet = state.getCurrentSpritesheet();
            if (sheet != null) {
                selectedBrushLabel.setText(sheet.getName() + " - Tile " + state.getSelectedBrush().getTileId());
            } else {
                selectedBrushLabel.setText("Tile " + state.getSelectedBrush().getTileId());
            }
        }
    }

    public Stage getStage() {
        return stage;
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        if (topPanel != null) {
            topPanel.setWidth(width);
            topPanel.setPosition(0, height - TOP_PANEL_HEIGHT);
        }
        if (leftPanel != null) {
            leftPanel.setHeight(height - TOP_PANEL_HEIGHT - PANEL_MARGIN * 2);
            leftPanel.setPosition(PANEL_MARGIN, PANEL_MARGIN);
        }
        if (rightPanel != null) {
            rightPanel.setHeight(height - TOP_PANEL_HEIGHT - PANEL_MARGIN * 2);
            rightPanel.setPosition(width - RIGHT_PANEL_WIDTH - PANEL_MARGIN, PANEL_MARGIN);
        }
    }

    public void dispose() {
        if (stage != null) stage.dispose();
    }
}