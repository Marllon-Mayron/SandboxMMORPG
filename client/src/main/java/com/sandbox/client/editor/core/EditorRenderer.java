package com.sandbox.client.editor.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.sandbox.client.editor.models.ChunkData;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.LayerType;
import com.sandbox.client.editor.models.SpritesheetData;
import com.sandbox.client.editor.models.TileRef;

public class EditorRenderer {
    private static final int TILE_SIZE = 32;
    private static final int CHUNK_SIZE = 32;

    private final EditorCamera editorCamera;
    private final IEditorScreen editorScreen;
    private final EditorState state;
    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;

    public EditorRenderer(EditorCamera editorCamera, IEditorScreen editorScreen, EditorState state) {
        this.editorCamera = editorCamera;
        this.editorScreen = editorScreen;
        this.state = state;
        this.batch = new SpriteBatch();
        this.shapeRenderer = new ShapeRenderer();
    }

    public void render() {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        editorCamera.update();

        batch.setProjectionMatrix(editorCamera.getCamera().combined);
        batch.begin();
        renderAllChunks();
        batch.end();

        renderGridAndBounds();
    }

    private void renderAllChunks() {
        float viewLeft = editorCamera.getCamera().position.x - editorCamera.getCamera().viewportWidth / 2;
        float viewRight = editorCamera.getCamera().position.x + editorCamera.getCamera().viewportWidth / 2;
        float viewBottom = editorCamera.getCamera().position.y - editorCamera.getCamera().viewportHeight / 2;
        float viewTop = editorCamera.getCamera().position.y + editorCamera.getCamera().viewportHeight / 2;

        int startChunkX = (int) Math.floor(viewLeft / (CHUNK_SIZE * TILE_SIZE)) - 1;
        int endChunkX = (int) Math.ceil(viewRight / (CHUNK_SIZE * TILE_SIZE)) + 1;
        int startChunkY = (int) Math.floor(viewBottom / (CHUNK_SIZE * TILE_SIZE)) - 1;
        int endChunkY = (int) Math.ceil(viewTop / (CHUNK_SIZE * TILE_SIZE)) + 1;

        for (LayerType layer : LayerType.values()) {
            for (int cx = startChunkX; cx <= endChunkX; cx++) {
                for (int cy = startChunkY; cy <= endChunkY; cy++) {
                    ChunkData chunkData = state.getChunks().get(cx + ":" + cy);
                    if (chunkData != null) {
                        renderChunkLayer(chunkData, cx, cy, layer);
                    }
                }
            }
        }
    }

    private void renderChunkLayer(ChunkData chunkData, int chunkX, int chunkY, LayerType layer) {
        float worldOffsetX = chunkX * CHUNK_SIZE * TILE_SIZE;
        float worldOffsetY = chunkY * CHUNK_SIZE * TILE_SIZE;

        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int y = 0; y < CHUNK_SIZE; y++) {
                TileRef tile = chunkData.getTile(layer, x, y);
                if (tile != null && tile.isValid() && tile.getTileId() >= 0) {

                    SpritesheetData sheet = state.getSpritesheets().get(tile.getSpritesheetPath());
                    if (sheet != null && tile.getTileId() < sheet.getTotalSprites()) {
                        batch.draw(sheet.getSprite(tile.getTileId()),
                                worldOffsetX + x * TILE_SIZE,
                                worldOffsetY + y * TILE_SIZE);
                    }
                }
            }
        }
    }

    private void renderGridAndBounds() {
        shapeRenderer.setProjectionMatrix(editorCamera.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        float viewLeft = editorCamera.getCamera().position.x - editorCamera.getCamera().viewportWidth / 2;
        float viewRight = editorCamera.getCamera().position.x + editorCamera.getCamera().viewportWidth / 2;
        float viewBottom = editorCamera.getCamera().position.y - editorCamera.getCamera().viewportHeight / 2;
        float viewTop = editorCamera.getCamera().position.y + editorCamera.getCamera().viewportHeight / 2;

        if (state.isShowGrid()) {
            shapeRenderer.setColor(0.4f, 0.4f, 0.5f, 0.8f);

            float chunkStartX = state.getCurrentChunkX() * CHUNK_SIZE * TILE_SIZE;
            float chunkStartY = state.getCurrentChunkY() * CHUNK_SIZE * TILE_SIZE;
            float chunkEndX = chunkStartX + CHUNK_SIZE * TILE_SIZE;
            float chunkEndY = chunkStartY + CHUNK_SIZE * TILE_SIZE;

            if (chunkEndX > viewLeft && chunkStartX < viewRight &&
                    chunkEndY > viewBottom && chunkStartY < viewTop) {

                for (float x = chunkStartX; x <= chunkEndX; x += TILE_SIZE) {
                    shapeRenderer.line(x, chunkStartY, x, chunkEndY);
                }
                for (float y = chunkStartY; y <= chunkEndY; y += TILE_SIZE) {
                    shapeRenderer.line(chunkStartX, y, chunkEndX, y);
                }
            }
        }

        if (state.isShowChunkBounds()) {
            int startChunkX = (int) Math.floor(viewLeft / (CHUNK_SIZE * TILE_SIZE));
            int endChunkX = (int) Math.ceil(viewRight / (CHUNK_SIZE * TILE_SIZE));
            int startChunkY = (int) Math.floor(viewBottom / (CHUNK_SIZE * TILE_SIZE));
            int endChunkY = (int) Math.ceil(viewTop / (CHUNK_SIZE * TILE_SIZE));

            for (int cx = startChunkX; cx <= endChunkX; cx++) {
                for (int cy = startChunkY; cy <= endChunkY; cy++) {
                    float x = cx * CHUNK_SIZE * TILE_SIZE;
                    float y = cy * CHUNK_SIZE * TILE_SIZE;
                    float w = CHUNK_SIZE * TILE_SIZE;
                    float h = CHUNK_SIZE * TILE_SIZE;

                    boolean isCurrent = (cx == state.getCurrentChunkX() && cy == state.getCurrentChunkY());
                    if (isCurrent) {
                        shapeRenderer.setColor(1f, 0.6f, 0.2f, 1f);
                        shapeRenderer.rect(x, y, w, h);
                    } else {
                        shapeRenderer.setColor(0.5f, 0.3f, 0.1f, 0.5f);
                        shapeRenderer.rect(x, y, w, h);
                    }
                }
            }
        }

        shapeRenderer.end();
    }

    public void dispose() {
        if (batch != null) batch.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}