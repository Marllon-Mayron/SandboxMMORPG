package com.sandbox.client.editor.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector3;

public class EditorCamera {
    private static final int TILE_SIZE = 32;
    private static final int CHUNK_SIZE = 32;
    private static final float MIN_ZOOM = 0.2f;
    private static final float MAX_ZOOM = 2f;
    private static final float ZOOM_SPEED = 0.1f;

    private OrthographicCamera camera;
    private float zoom;
    private float dragX, dragY;
    private boolean isPanning;

    public EditorCamera() {
        this.camera = new OrthographicCamera();
        this.zoom = 0.5f;
        this.isPanning = false;
    }

    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);
        camera.zoom = zoom;
        camera.update();
    }

    public void centerOnChunk(int chunkX, int chunkY) {
        float centerX = (chunkX * CHUNK_SIZE * TILE_SIZE) + (CHUNK_SIZE * TILE_SIZE) / 2f;
        float centerY = (chunkY * CHUNK_SIZE * TILE_SIZE) + (CHUNK_SIZE * TILE_SIZE) / 2f;
        camera.position.set(centerX, centerY, 0);
        camera.update();
    }

    public void startPan(int screenX, int screenY) {
        isPanning = true;
        dragX = screenX;
        dragY = screenY;
    }

    public void pan(int screenX, int screenY) {
        if (isPanning) {
            float deltaX = (screenX - dragX) * camera.zoom;
            float deltaY = (screenY - dragY) * camera.zoom;
            camera.translate(-deltaX, deltaY);
            dragX = screenX;
            dragY = screenY;
        }
    }

    public void stopPan() {
        isPanning = false;
    }

    public void zoom(float amount, int screenX, int screenY) {
        Vector3 mousePos = new Vector3(screenX, screenY, 0);
        camera.unproject(mousePos);

        float newZoom = camera.zoom + (amount * ZOOM_SPEED);
        newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newZoom));
        camera.zoom = newZoom;
        zoom = newZoom;

        Vector3 newMousePos = new Vector3(screenX, screenY, 0);
        camera.unproject(newMousePos);
        camera.position.add(mousePos.x - newMousePos.x, mousePos.y - newMousePos.y, 0);
    }

    public OrthographicCamera getCamera() { return camera; }
    public float getZoom() { return zoom; }
    public boolean isPanning() { return isPanning; }

    public Vector3 unproject(int screenX, int screenY) {
        Vector3 pos = new Vector3(screenX, screenY, 0);
        camera.unproject(pos);
        return pos;
    }

    public void update() {
        camera.update();
    }
}