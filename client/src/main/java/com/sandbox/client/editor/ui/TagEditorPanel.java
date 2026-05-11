package com.sandbox.client.editor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.common.sandbox.model.TileTag;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.TileRef;

public class TagEditorPanel {
    private Table table;
    private Skin skin;
    private EditorState state;
    private TextField tagNameField;
    private SelectBox<String> tagPresetSelect;
    private Label currentTagLabel;

    public TagEditorPanel(Skin skin, EditorState state) {
        this.skin = skin;
        this.state = state;
        createPanel();
    }

    private void createPanel() {
        table = new Table();

        Label sectionLabel = new Label("TILE TAGS", skin, "section");
        sectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        table.add(sectionLabel).left().padBottom(5);
        table.row();

        // Current tag display
        currentTagLabel = new Label("Current tag: none", skin);
        table.add(currentTagLabel).left().padBottom(10);
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
        presetTable.add(tagPresetSelect).width(100);
        table.add(presetTable).left().padBottom(10);
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
        table.add(customTable).left().padBottom(10);
        table.row();

        // Clear tag button
        TextButton clearTagBtn = new TextButton("Clear Tag", skin, "default");
        clearTagBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                clearCurrentTileTag();
            }
        });
        table.add(clearTagBtn).width(340).padBottom(5);
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
    }

    private void applyCustomTag(String tagName) {
        TileTag tag = new TileTag(tagName);
        applyTagToBrush(tag);
    }

    private void applyTagToBrush(TileTag tag) {
        TileRef brush = state.getSelectedBrush();
        if (brush != null && brush.isValid()) {
            brush.setTag(tag);
            String tagInfo = tag.getName();
            if (tag.isSolid()) {
                tagInfo += " (blocks movement)";
            } else if (tag.isWater()) {
                tagInfo += " (slows movement)";
            } else if (tag.isLava()) {
                tagInfo += " (damages player)";
            }
            currentTagLabel.setText("Current tag: " + tagInfo);
        }
    }

    private void clearCurrentTileTag() {
        TileRef brush = state.getSelectedBrush();
        if (brush != null) {
            brush.setTag(new TileTag("default"));
            currentTagLabel.setText("Current tag: none");
            tagNameField.setText("");
            tagPresetSelect.setSelected("none");
        }
    }

    public Table getTable() { return table; }
}