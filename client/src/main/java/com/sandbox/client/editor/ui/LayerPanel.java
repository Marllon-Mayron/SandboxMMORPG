package com.sandbox.client.editor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.LayerType;

public class LayerPanel {
    private Table table;
    private Skin skin;
    private EditorState state;
    private Label currentLayerLabel;
    private TextButton[] layerButtons;

    public LayerPanel(Skin skin, EditorState state) {
        this.skin = skin;
        this.state = state;
        this.layerButtons = new TextButton[3];
        createPanel();
    }

    private void createPanel() {
        table = new Table();

        Label sectionLabel = new Label("CURRENT LAYER", skin, "section");
        sectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        table.add(sectionLabel).left().padBottom(5);
        table.row();

        Table buttonTable = new Table();

        for (int i = 0; i < LayerType.values().length; i++) {
            final LayerType layer = LayerType.values()[i];
            boolean isCurrent = (state.getCurrentLayer() == layer);
            layerButtons[i] = new TextButton(layer.name, skin, isCurrent ? "warning" : "default");
            layerButtons[i].addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentLayer(layer);
                    updateCurrentLayer();
                }
            });
            buttonTable.add(layerButtons[i]).width(100).padRight(5);
        }

        table.add(buttonTable).left().padBottom(10);
        table.row();

        currentLayerLabel = new Label("Editing: Ground", skin);
        currentLayerLabel.setColor(state.getCurrentLayer().color);
        table.add(currentLayerLabel).left().padBottom(5);
    }

    public void updateCurrentLayer() {
        for (int i = 0; i < layerButtons.length; i++) {
            LayerType layer = LayerType.values()[i];
            String style = (state.getCurrentLayer() == layer) ? "warning" : "default";
            layerButtons[i].setStyle(skin.get(style, TextButton.TextButtonStyle.class));
        }
        currentLayerLabel.setText("Editing: " + state.getCurrentLayer().name);
        currentLayerLabel.setColor(state.getCurrentLayer().color);
    }

    public Table getTable() { return table; }
}