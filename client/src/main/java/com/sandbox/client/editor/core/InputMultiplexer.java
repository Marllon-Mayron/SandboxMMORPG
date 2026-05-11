package com.sandbox.client.editor.core;

import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.scenes.scene2d.Stage;

/**
 * Processa inputs primeiro no Stage (UI) e depois no handler do editor
 */
public class InputMultiplexer implements InputProcessor {
    private final Stage stage;
    private final InputProcessor editorHandler;

    public InputMultiplexer(Stage stage, InputProcessor editorHandler) {
        this.stage = stage;
        this.editorHandler = editorHandler;
    }

    @Override
    public boolean keyDown(int keycode) {
        // Primeiro tenta processar no Stage (UI)
        if (stage.keyDown(keycode)) return true;
        // Depois no editor
        return editorHandler.keyDown(keycode);
    }

    @Override
    public boolean keyUp(int keycode) {
        if (stage.keyUp(keycode)) return true;
        return editorHandler.keyUp(keycode);
    }

    @Override
    public boolean keyTyped(char character) {
        if (stage.keyTyped(character)) return true;
        return editorHandler.keyTyped(character);
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // Primeiro tenta processar no Stage (UI)
        if (stage.touchDown(screenX, screenY, pointer, button)) return true;
        // Depois no editor (apenas se não clicou na UI)
        return editorHandler.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (stage.touchUp(screenX, screenY, pointer, button)) return true;
        return editorHandler.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (stage.touchDragged(screenX, screenY, pointer)) return true;
        return editorHandler.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        if (stage.mouseMoved(screenX, screenY)) return true;
        return editorHandler.mouseMoved(screenX, screenY);
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        if (stage.scrolled(amountX, amountY)) return true;
        return editorHandler.scrolled(amountX, amountY);
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (stage.touchCancelled(screenX, screenY, pointer, button)) return true;
        return editorHandler.touchCancelled(screenX, screenY, pointer, button);
    }
}