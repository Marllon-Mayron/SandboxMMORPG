package com.sandbox.client.editor.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.core.EditorCamera;
import com.sandbox.client.editor.models.ChunkData;
import com.sandbox.client.editor.models.EditorState;

public class ChunkPanel {
    private Table table;
    private Skin skin;
    private SandboxClient game;
    private EditorState state;
    private EditorCamera editorCamera;
    private Table chunkListTable;
    private ScrollPane chunkScroll;
    private TextField chunkXField, chunkYField;
    private TextField newChunkXField, newChunkYField;

    public ChunkPanel(Skin skin, SandboxClient game, EditorState state, EditorCamera editorCamera) {
        this.skin = skin;
        this.game = game;
        this.state = state;
        this.editorCamera = editorCamera;
        createPanel();
    }

    private void createPanel() {
        table = new Table();

        Label sectionLabel = new Label("CHUNKS", skin, "section");
        sectionLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
        table.add(sectionLabel).left().padBottom(5);
        table.row();

        // Create new chunk
        Table createTable = new Table();
        createTable.add(new Label("New:", skin)).padRight(5);
        newChunkXField = new TextField("0", skin);
        newChunkXField.setWidth(50);
        createTable.add(newChunkXField).width(50).padRight(5);
        createTable.add(new Label(":", skin)).padRight(5);
        newChunkYField = new TextField("0", skin);
        newChunkYField.setWidth(50);
        createTable.add(newChunkYField).width(50).padRight(10);

        TextButton createBtn = new TextButton("Create", skin, "success");
        createBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int x = Integer.parseInt(newChunkXField.getText());
                    int y = Integer.parseInt(newChunkYField.getText());
                    state.createChunk(x, y);
                    state.setCurrentChunk(x, y);
                    refresh();
                    editorCamera.centerOnChunk(x, y);
                } catch (NumberFormatException e) {
                    // Invalid input
                }
            }
        });
        createTable.add(createBtn).width(60);
        table.add(createTable).left().padBottom(10);
        table.row();

        // Chunk list
        chunkListTable = new Table();
        chunkScroll = new ScrollPane(chunkListTable, skin);
        chunkScroll.setHeight(100);
        table.add(chunkScroll).width(340).height(100).padBottom(10);
        table.row();

        // Navigate
        Table navTable = new Table();
        navTable.add(new Label("Go to:", skin)).padRight(5);
        chunkXField = new TextField("0", skin);
        chunkXField.setWidth(50);
        navTable.add(chunkXField).width(50).padRight(5);
        navTable.add(new Label(":", skin)).padRight(5);
        chunkYField = new TextField("0", skin);
        chunkYField.setWidth(50);
        navTable.add(chunkYField).width(50).padRight(10);

        TextButton goBtn = new TextButton("Go", skin);
        goBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    int x = Integer.parseInt(chunkXField.getText());
                    int y = Integer.parseInt(chunkYField.getText());
                    if (state.getChunks().containsKey(x + ":" + y)) {
                        state.setCurrentChunk(x, y);
                        refresh();
                        editorCamera.centerOnChunk(x, y);
                    }
                } catch (NumberFormatException e) {
                    // Invalid input
                }
            }
        });
        navTable.add(goBtn).width(50);
        table.add(navTable).left().padBottom(10);
        table.row();

        // Delete button
        TextButton deleteBtn = new TextButton("Delete Current Chunk", skin, "danger");
        deleteBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                state.deleteChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
                refresh();
                editorCamera.centerOnChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
            }
        });
        table.add(deleteBtn).width(340).padBottom(10);

        refresh();
    }

    public void refresh() {
        chunkListTable.clear();

        for (ChunkData chunk : state.getChunks().values()) {
            Table itemTable = new Table();

            boolean isCurrent = (chunk.getX() == state.getCurrentChunkX() && chunk.getY() == state.getCurrentChunkY());
            Label chunkName = new Label("[" + chunk.getX() + "," + chunk.getY() + "]", skin);
            if (isCurrent) {
                chunkName.setColor(com.badlogic.gdx.graphics.Color.GOLD);
            }
            itemTable.add(chunkName).left().expandX();

            TextButton selectBtn = new TextButton("Select", skin, "primary");
            final int fx = chunk.getX(), fy = chunk.getY();
            selectBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.setCurrentChunk(fx, fy);
                    refresh();
                    editorCamera.centerOnChunk(fx, fy);
                }
            });
            itemTable.add(selectBtn).width(60).padRight(5);

            TextButton deleteBtn = new TextButton("X", skin, "danger");
            deleteBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    state.deleteChunk(fx, fy);
                    refresh();
                    editorCamera.centerOnChunk(state.getCurrentChunkX(), state.getCurrentChunkY());
                }
            });
            itemTable.add(deleteBtn).width(30);

            chunkListTable.add(itemTable).width(320).padBottom(3);
            chunkListTable.row();
        }

        chunkXField.setText(String.valueOf(state.getCurrentChunkX()));
        chunkYField.setText(String.valueOf(state.getCurrentChunkY()));
        newChunkXField.setText("0");
        newChunkYField.setText("0");
    }

    public void updateChunkDisplay(int x, int y) {
        chunkXField.setText(String.valueOf(x));
        chunkYField.setText(String.valueOf(y));
    }

    public Table getTable() { return table; }
}