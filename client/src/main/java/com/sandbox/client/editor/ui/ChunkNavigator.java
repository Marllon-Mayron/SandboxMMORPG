package com.sandbox.client.editor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;

import java.util.function.BiConsumer;

public class ChunkNavigator {
    private Table container;
    private TextField chunkXField, chunkYField;
    private Label currentChunkLabel;
    private BiConsumer<Integer, Integer> onChunkChanged;

    public ChunkNavigator(Skin skin, BiConsumer<Integer, Integer> onChunkChanged) {
        this.onChunkChanged = onChunkChanged;
        this.container = new Table(skin);
        createNavigator(skin);
    }

    private void createNavigator(Skin skin) {
        Table formTable = new Table();

        formTable.add(new Label("X:", skin)).left().padRight(5);
        chunkXField = new TextField("0", skin);
        formTable.add(chunkXField).width(60).padRight(15);

        formTable.add(new Label("Y:", skin)).left().padRight(5);
        chunkYField = new TextField("0", skin);
        formTable.add(chunkYField).width(60);
        formTable.row();

        TextButton goButton = new TextButton("Go to Chunk", skin);
        goButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int x = Integer.parseInt(chunkXField.getText());
                    int y = Integer.parseInt(chunkYField.getText());
                    if (onChunkChanged != null) {
                        onChunkChanged.accept(x, y);
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
            }
        });
        formTable.add(goButton).colspan(4).width(120).padTop(5);

        container.add(formTable);
    }

    public Actor getWidget() {
        return container;
    }
}