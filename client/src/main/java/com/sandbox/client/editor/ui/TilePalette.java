package com.sandbox.client.editor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.SpritesheetData;
import com.sandbox.client.editor.models.TileRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TilePalette {
    private static final Logger logger = LoggerFactory.getLogger(TilePalette.class);

    private ScrollPane scrollPane;
    private Table table;
    private Skin skin;
    private EditorState state;
    private Label selectedSpriteLabel;

    public TilePalette(Skin skin, EditorState state, Label selectedSpriteLabel) {
        this.skin = skin;
        this.state = state;
        this.selectedSpriteLabel = selectedSpriteLabel;
        this.table = new Table();
        this.scrollPane = new ScrollPane(table, skin);
        refresh();
    }

    public void refresh() {
        table.clear();

        logger.info("Refreshing TilePalette");

        if (state.getCurrentSpritesheet() == null) {
            Label emptyLabel = new Label("No spritesheet selected.\nClick 'Use' on a spritesheet above.", skin, "status");
            emptyLabel.setWrap(true);
            table.add(emptyLabel).width(320).pad(10);
            logger.warn("No spritesheet selected");
            return;
        }

        int cols = 6;
        SpritesheetData sheet = state.getCurrentSpritesheet();

        logger.info("Loading spritesheet: {} with {} sprites", sheet.getName(), sheet.getTotalSprites());

        Label headerLabel = new Label("--- " + sheet.getName() + " (" + sheet.getPath() + ") ---", skin, "section");
        headerLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        table.add(headerLabel).colspan(cols).center().padTop(10).padBottom(5);
        table.row();

        int totalSprites = sheet.getTotalSprites();

        for (int i = 0; i < totalSprites; i++) {
            final int tileId = i;
            final String sheetPath = sheet.getPath();  // ✅ Usar PATH, não nome!

            try {
                ImageButton spriteBtn = new ImageButton(new TextureRegionDrawable(sheet.getSprite(i)));
                spriteBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {

                        TileRef selectedBrush = new TileRef(sheetPath, tileId);
                        state.setSelectedBrush(selectedBrush);
                        selectedSpriteLabel.setText("Selected: " + sheetPath + " [" + (tileId % sheet.getCols()) + "," + (tileId / sheet.getCols()) + "]");
                        logger.info("Selected brush: path={}, tileId={}", sheetPath, tileId);
                    }
                });

                table.add(spriteBtn).size(40, 40).pad(2);

                if ((i + 1) % cols == 0) {
                    table.row();
                }
            } catch (Exception e) {
                logger.error("Error creating button for tile {}: {}", i, e.getMessage());
            }
        }

        if (totalSprites == 0) {
            Label emptyLabel = new Label("Spritesheet has no sprites.\nCheck image size (should be multiple of 32)", skin, "status");
            emptyLabel.setWrap(true);
            table.add(emptyLabel).colspan(cols).center().pad(10);
        }

        logger.info("TilePalette refreshed with {} sprites", totalSprites);
    }

    public ScrollPane getScrollPane() {
        return scrollPane;
    }
}