package com.sandbox.client.editor.models;

import com.common.sandbox.model.TileTag;

public class ChunkData {
    private int x;
    private int y;
    private TileRef[][][] layers; // [layer][x][y]

    public ChunkData(int x, int y) {
        this.x = x;
        this.y = y;
        this.layers = new TileRef[3][32][32];
        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 32; i++) {
                for (int j = 0; j < 32; j++) {
                    layers[layer][i][j] = new TileRef();
                }
            }
        }
    }

    public int getX() { return x; }
    public int getY() { return y; }

    public void setTile(LayerType layer, int localX, int localY, String spritesheetPath, int tileId) {
        setTile(layer, localX, localY, new TileRef(spritesheetPath, tileId));
    }

    public void setTile(LayerType layer, int localX, int localY, TileRef tile) {
        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            layers[layer.id][localX][localY] = tile;
        }
    }

    public TileRef getTile(LayerType layer, int localX, int localY) {
        if (localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            return layers[layer.id][localX][localY];
        }
        return new TileRef();
    }

    public void clearLayer(LayerType layer) {
        for (int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                layers[layer.id][i][j] = new TileRef();
            }
        }
    }

    public void setTagForTile(LayerType layer, int localX, int localY, TileTag tag) {
        TileRef tile = getTile(layer, localX, localY);
        if (tile != null) {
            tile.setTag(tag);
        }
    }

    public String getKey() { return x + ":" + y; }
}