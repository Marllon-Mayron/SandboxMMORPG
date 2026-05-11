package com.sandbox.client.editor.models;

import java.util.HashMap;
import java.util.Map;

public class EditorState {
    private Map<String, ChunkData> chunks;
    private int currentChunkX;
    private int currentChunkY;
    private LayerType currentLayer;
    private TileRef selectedBrush;
    private Map<String, SpritesheetData> spritesheets;
    private SpritesheetData currentSpritesheet;
    private boolean showGrid;
    private boolean showChunkBounds;
    private float zoom;
    private ToolType currentTool;

    public EditorState() {
        this.chunks = new HashMap<>();
        this.currentLayer = LayerType.GROUND;
        this.selectedBrush = new TileRef();
        this.spritesheets = new HashMap<>();
        this.showGrid = true;
        this.showChunkBounds = true;
        this.zoom = 0.5f;
        this.currentTool = ToolType.BRUSH;
    }

    public Map<String, ChunkData> getChunks() { return chunks; }
    public int getCurrentChunkX() { return currentChunkX; }
    public void setCurrentChunkX(int x) { this.currentChunkX = x; }
    public int getCurrentChunkY() { return currentChunkY; }
    public void setCurrentChunkY(int y) { this.currentChunkY = y; }
    public LayerType getCurrentLayer() { return currentLayer; }
    public void setCurrentLayer(LayerType layer) { this.currentLayer = layer; }
    public TileRef getSelectedBrush() { return selectedBrush; }
    public void setSelectedBrush(TileRef brush) { this.selectedBrush = brush; }
    public Map<String, SpritesheetData> getSpritesheets() { return spritesheets; }
    public SpritesheetData getCurrentSpritesheet() { return currentSpritesheet; }
    public void setCurrentSpritesheet(SpritesheetData sheet) { this.currentSpritesheet = sheet; }
    public boolean isShowGrid() { return showGrid; }
    public void setShowGrid(boolean show) { this.showGrid = show; }
    public boolean isShowChunkBounds() { return showChunkBounds; }
    public void setShowChunkBounds(boolean show) { this.showChunkBounds = show; }
    public float getZoom() { return zoom; }
    public void setZoom(float zoom) { this.zoom = zoom; }
    public ToolType getCurrentTool() { return currentTool; }
    public void setCurrentTool(ToolType tool) { this.currentTool = tool; }

    public ChunkData getCurrentChunk() {
        return chunks.get(currentChunkX + ":" + currentChunkY);
    }

    public void createChunk(int x, int y) {
        String key = x + ":" + y;
        if (!chunks.containsKey(key)) {
            chunks.put(key, new ChunkData(x, y));
        }
    }

    public void deleteChunk(int x, int y) {
        String key = x + ":" + y;
        if (chunks.containsKey(key) && chunks.size() > 1) {
            chunks.remove(key);
            if (currentChunkX == x && currentChunkY == y) {
                ChunkData first = chunks.values().iterator().next();
                currentChunkX = first.getX();
                currentChunkY = first.getY();
            }
        }
    }

    public void setCurrentChunk(int x, int y) {
        if (chunks.containsKey(x + ":" + y)) {
            currentChunkX = x;
            currentChunkY = y;
        }
    }
}