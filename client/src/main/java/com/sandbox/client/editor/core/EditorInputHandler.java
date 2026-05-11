package com.sandbox.client.editor.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector3;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.LayerType;
import com.sandbox.client.editor.models.TileRef;
import com.sandbox.client.editor.ui.EditorWindow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EditorInputHandler implements InputProcessor {
    private static final Logger logger = LoggerFactory.getLogger(EditorInputHandler.class);

    private final EditorCamera editorCamera;
    private final EditorWindow editorWindow;
    private final EditorRenderer renderer;
    private final EditorState state;

    public EditorInputHandler(EditorCamera editorCamera, EditorWindow editorWindow, EditorRenderer renderer, EditorState state) {
        this.editorCamera = editorCamera;
        this.editorWindow = editorWindow;
        this.renderer = renderer;
        this.state = state;
    }

    @Override
    public boolean keyDown(int keycode) {
        logger.debug("Key pressed: {}", keycode);

        switch (keycode) {
            case Input.Keys.NUM_1:
                state.setCurrentLayer(LayerType.GROUND);
                editorWindow.updateLayerDisplay();
                logger.info("Layer changed to GROUND");
                return true;
            case Input.Keys.NUM_2:
                state.setCurrentLayer(LayerType.DECORATION);
                editorWindow.updateLayerDisplay();
                logger.info("Layer changed to DECORATION");
                return true;
            case Input.Keys.NUM_3:
                state.setCurrentLayer(LayerType.CEILING);
                editorWindow.updateLayerDisplay();
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
                editorWindow.refreshUI();
                logger.info("UI refreshed");
                return true;
            case Input.Keys.S:
                if (Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT)) {
                    editorWindow.saveMap();
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
        // Verificar se o mouse está sobre a UI
        boolean mouseOverUI = editorWindow.isMouseOver(screenX, screenY);

        logger.debug("Touch down - screen: ({},{}), button: {}, overUI: {}", screenX, screenY, button, mouseOverUI);

        // Se mouse estiver sobre a UI, NÃO processar no editor
        if (mouseOverUI) {
            logger.debug("Mouse over UI, ignoring editor input");
            return false;
        }

        if (button == Input.Buttons.MIDDLE) {
            editorCamera.startPan(screenX, screenY);
            logger.debug("Started panning");
            return true;
        }

        if (button == Input.Buttons.LEFT && state.getSelectedBrush().isValid()) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);
            logger.debug("Placing tile at world: ({}, {})", worldPos.x, worldPos.y);
            placeTile(worldPos.x, worldPos.y);
            return true;
        }

        if (button == Input.Buttons.RIGHT) {
            Vector3 worldPos = editorCamera.unproject(screenX, screenY);
            logger.debug("Erasing tile at world: ({}, {})", worldPos.x, worldPos.y);
            eraseTile(worldPos.x, worldPos.y);
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            editorCamera.stopPan();
            logger.debug("Stopped panning");
            return true;
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (button == Input.Buttons.MIDDLE) {
            editorCamera.stopPan();
            return true;
        }
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (editorCamera.isPanning()) {
            editorCamera.pan(screenX, screenY);
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
        boolean mouseOverUI = editorWindow.isMouseOver(Gdx.input.getX(), Gdx.input.getY());
        if (!mouseOverUI) {
            editorCamera.zoom(amountY, Gdx.input.getX(), Gdx.input.getY());
            logger.debug("Zoomed: {}", editorCamera.getZoom());
            return true;
        }
        return false;
    }

    private void placeTile(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) {
            logger.warn("No current chunk selected");
            return;
        }

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) ((worldX - chunkOffsetX) / 32);
        int localY = (int) ((worldY - chunkOffsetY) / 32);

        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            TileRef brush = state.getSelectedBrush();
            if (brush != null && brush.isValid()) {

                state.getCurrentChunk().setTile(
                        state.getCurrentLayer(),
                        localX,
                        localY,
                        brush.getSpritesheetPath(),
                        brush.getTileId()
                );
                logger.debug("Placed tile at local: ({},{}), path={}, tileId={}",
                        localX, localY, brush.getSpritesheetPath(), brush.getTileId());
            }
        } else {
            logger.debug("Tile placement out of bounds: local ({}, {})", localX, localY);
        }
    }

    private void eraseTile(float worldX, float worldY) {
        if (state.getCurrentChunk() == null) return;

        int chunkOffsetX = state.getCurrentChunkX() * 32 * 32;
        int chunkOffsetY = state.getCurrentChunkY() * 32 * 32;

        int localX = (int) ((worldX - chunkOffsetX) / 32);
        int localY = (int) ((worldY - chunkOffsetY) / 32);

        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            state.getCurrentChunk().setTile(state.getCurrentLayer(), localX, localY, "", 0);
            logger.debug("Erased tile at local: ({}, {})", localX, localY);
        }
    }
}