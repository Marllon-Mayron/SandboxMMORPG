// File: EditorInputHandler.java - VERSÃO CORRIGIDA
package com.sandbox.client.editor.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.LayerType;
import com.sandbox.client.editor.models.TileRef;
import com.sandbox.client.editor.models.ToolType;
import com.sandbox.client.editor.utils.FloodFill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorInputHandler implements InputProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EditorInputHandler.class);

    private final EditorCamera editorCamera;
    private final IEditorScreen editorScreen;
    private final EditorRenderer renderer;
    private final EditorState state;

    // Drag painting
    private boolean isPainting = false;
    private boolean isErasing = false;
    private int lastPlacedX = -1;
    private int lastPlacedY = -1;
    private boolean isPanning = false;

    public EditorInputHandler(EditorCamera editorCamera, IEditorScreen editorScreen, EditorRenderer renderer, EditorState state) {
        this.editorCamera = editorCamera;
        this.editorScreen = editorScreen;
        this.renderer = renderer;
        this.state = state;
    }

    @Override
    public boolean keyDown(int keycode) {
        logger.debug("Key pressed: {}", keycode);

        switch (keycode) {
            case Input.Keys.NUM_1:
                state.setCurrentLayer(LayerType.GROUND);
                editorScreen.refreshUI();
                logger.info("Layer changed to GROUND");
                return true;
            case Input.Keys.NUM_2:
                state.setCurrentLayer(LayerType.DECORATION);
                editorScreen.refreshUI();
                logger.info("Layer changed to DECORATION");
                return true;
            case Input.Keys.NUM_3:
                state.setCurrentLayer(LayerType.CEILING);
                editorScreen.refreshUI();
                logger.info("Layer changed to CEILING");
                return true;
            case Input.Keys.DEL:
                if (state.getCurrentChunk() != null) {
                    state.getCurrentChunk().clearLayer(state.getCurrentLayer());
                    logger.info("Cleared layer: {}", state.getCurrentLayer().name);
                }
                return true;
            case Input.Keys.H:
                state.setShowGrid(!state.isShowGrid());
                logger.info("Show grid: {}", state.isShowGrid());
                return true;
            case Input.Keys.B:
                state.setShowChunkBounds(!state.isShowChunkBounds());
                logger.info("Show chunk bounds: {}", state.isShowChunkBounds());
                return true;
            case Input.Keys.F5:
                editorScreen.refreshUI();
                logger.info("UI refreshed");
                return true;
            case Input.Keys.LEFT_BRACKET:
                state.setCurrentTool(ToolType.BRUSH);
                editorScreen.refreshUI();
                logger.info("Switched to Brush tool");
                return true;
            case Input.Keys.RIGHT_BRACKET:
                state.setCurrentTool(ToolType.BUCKET);
                editorScreen.refreshUI();
                logger.info("Switched to Bucket tool");
                return true;
            case Input.Keys.S:
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    if (editorScreen instanceof com.sandbox.client.editor.MapEditorScreen) {
                        ((com.sandbox.client.editor.MapEditorScreen) editorScreen).saveMap();
                    }
                    return true;
                }
                break;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Converter coordenadas para sistema Y-up (LibGDX)
        int actualY = Gdx.graphics.getHeight() - screenY;
        boolean mouseOverUI = editorScreen.isMouseOverUI(screenX, actualY);

        logger.debug("Touch down - screen: ({},{}), actualY: {}, button: {}, overUI: {}",
                screenX, screenY, actualY, button, mouseOverUI);

        // Se mouse está sobre UI, não processar inputs do editor
        if (mouseOverUI) {
            logger.debug("Mouse over UI, ignoring editor input");
            return false;
        }

        // Middle button - pan
        if (button == Input.Buttons.MIDDLE) {
            editorCamera.startPan(screenX, screenY);
            isPanning = true;
            logger.debug("Started panning");
            return true;
        }

        // Left button - paint
        if (button == Input.Buttons.LEFT) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);

            if (state.getCurrentTool() == ToolType.BUCKET) {
                applyBucket(worldPos.x, worldPos.y);
            } else if (state.getCurrentTool() == ToolType.BRUSH && state.getSelectedBrush().isValid()) {
                isPainting = true;
                isErasing = false;
                lastPlacedX = -1;
                lastPlacedY = -1;
                placeTile(worldPos.x, worldPos.y);
            }
            return true;
        }

        // Right button - erase
        if (button == Input.Buttons.RIGHT) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);

            if (state.getCurrentTool() == ToolType.BUCKET) {
                applyBucketErase(worldPos.x, worldPos.y);
            } else if (state.getCurrentTool() == ToolType.BRUSH) {
                isErasing = true;
                isPainting = false;
                lastPlacedX = -1;
                lastPlacedY = -1;
                eraseTile(worldPos.x, worldPos.y);
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            editorCamera.stopPan();
            isPanning = false;
            logger.debug("Stopped panning");
            return true;
        }

        if (button == Input.Buttons.LEFT || button == Input.Buttons.RIGHT) {
            isPainting = false;
            isErasing = false;
            lastPlacedX = -1;
            lastPlacedY = -1;
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            editorCamera.stopPan();
            isPanning = false;
            return true;
        }
        isPainting = false;
        isErasing = false;
        lastPlacedX = -1;
        lastPlacedY = -1;
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        // Se está panning, mover câmera
        if (isPanning) {
            editorCamera.pan(screenX, screenY);
            return true;
        }

        // Converter coordenadas para sistema Y-up
        int actualY = Gdx.graphics.getHeight() - screenY;
        boolean mouseOverUI = editorScreen.isMouseOverUI(screenX, actualY);

        if (mouseOverUI) {
            return false;
        }

        // Paint dragging
        if (isPainting && state.getCurrentTool() == ToolType.BRUSH) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);
            placeTile(worldPos.x, worldPos.y);
            return true;
        }

        // Erase dragging
        if (isErasing && state.getCurrentTool() == ToolType.BRUSH) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);
            eraseTile(worldPos.x, worldPos.y);
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        int mouseX = Gdx.input.getX();
        int mouseY = Gdx.graphics.getHeight() - Gdx.input.getY();
        boolean mouseOverUI = editorScreen.isMouseOverUI(mouseX, mouseY);

        if (!mouseOverUI) {
            editorCamera.zoom(amountY, mouseX, mouseY);
            logger.debug("Zoomed: {}", editorCamera.getZoom());
            return true;
        }
        return false;
    }

    private void applyBucket(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) {
            logger.warn("No current chunk selected");
            return;
        }

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) Math.floor((worldX - chunkOffsetX) / 32);
        int localY = (int) Math.floor((worldY - chunkOffsetY) / 32);

        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            TileRef brush = state.getSelectedBrush();
            if (brush != null && brush.isValid()) {
                FloodFill.fill(state.getCurrentChunk(), state.getCurrentLayer(), localX, localY, brush);
                logger.debug("Flood fill at local: ({},{})", localX, localY);
            }
        }
    }

    private void applyBucketErase(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) return;

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) Math.floor((worldX - chunkOffsetX) / 32);
        int localY = (int) Math.floor((worldY - chunkOffsetY) / 32);

        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            TileRef emptyTile = new TileRef("", -1);
            FloodFill.fill(state.getCurrentChunk(), state.getCurrentLayer(), localX, localY, emptyTile);
            logger.debug("Flood fill erase at local: ({},{})", localX, localY);
        }
    }

    private void placeTile(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) {
            return;
        }

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) Math.floor((worldX - chunkOffsetX) / 32);
        int localY = (int) Math.floor((worldY - chunkOffsetY) / 32);

        if (localX < 0 || localX >= 32 || localY < 0 || localY >= 32) {
            return;
        }

        if (isPainting && lastPlacedX == localX && lastPlacedY == localY) {
            return;
        }

        TileRef brush = state.getSelectedBrush();
        if (brush != null && brush.isValid()) {
            TileRef tileToPlace = new TileRef(brush.getSpritesheetPath(), brush.getTileId());
            tileToPlace.setTag(brush.getTag());

            state.getCurrentChunk().setTile(
                    state.getCurrentLayer(),
                    localX,
                    localY,
                    tileToPlace
            );
            lastPlacedX = localX;
            lastPlacedY = localY;
        }
    }

    private void eraseTile(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) return;

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) Math.floor((worldX - chunkOffsetX) / 32);
        int localY = (int) Math.floor((worldY - chunkOffsetY) / 32);

        if (localX < 0 || localX >= 32 || localY < 0 || localY >= 32) {
            return;
        }

        if (isErasing && lastPlacedX == localX && lastPlacedY == localY) {
            return;
        }

        state.getCurrentChunk().setTile(state.getCurrentLayer(), localX, localY, new TileRef("", -1));
        lastPlacedX = localX;
        lastPlacedY = localY;
    }
}