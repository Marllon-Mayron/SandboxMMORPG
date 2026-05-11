package com.sandbox.client.editor.models;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class TilesetData {
    public String name;
    public String path;
    public Texture texture;
    public TextureRegion[][] tiles;
    public int tileWidth = 32;
    public int tileHeight = 32;
    public int rows;
    public int cols;

    public TilesetData(String name, String path, Texture texture, int tileWidth, int tileHeight) {
        this.name = name;
        this.path = path;
        this.texture = texture;
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.cols = texture.getWidth() / tileWidth;
        this.rows = texture.getHeight() / tileHeight;
        this.tiles = TextureRegion.split(texture, tileWidth, tileHeight);
    }

    public int getTotalTiles() {
        return rows * cols;
    }

    public TextureRegion getTile(int index) {
        if (index < 0 || index >= getTotalTiles()) return null;
        int row = index / cols;
        int col = index % cols;
        return tiles[row][col];
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }
}