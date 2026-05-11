package com.common.sandbox.model;

import java.io.Serializable;

public class Chunk implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int SIZE = 32;

    public int chunkX;
    public int chunkY;
    private WorldTile[][] tiles;

    public Chunk() {
        this.chunkX = 0;
        this.chunkY = 0;
        this.tiles = new WorldTile[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tiles[x][y] = new WorldTile();
            }
        }
    }

    public Chunk(int chunkX, int chunkY) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.tiles = new WorldTile[SIZE][SIZE];
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tiles[x][y] = new WorldTile();
            }
        }
    }

    public WorldTile getTile(int x, int y) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            WorldTile tile = tiles[x][y];
            return tile != null ? tile : new WorldTile();
        }
        return new WorldTile();
    }

    public void setTile(int x, int y, String spritesheetPath, int tileId, boolean solid, String tag) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            tiles[x][y] = new WorldTile(spritesheetPath, tileId, solid, tag);
        }
    }

    public void setTile(int x, int y, WorldTile tile) {
        if (x >= 0 && x < SIZE && y >= 0 && y < SIZE) {
            tiles[x][y] = tile;
        }
    }

    public WorldTile[][] getTiles() { return tiles; }
    public void setTiles(WorldTile[][] tiles) { this.tiles = tiles; }
    public int getChunkX() { return chunkX; }
    public void setChunkX(int chunkX) { this.chunkX = chunkX; }
    public int getChunkY() { return chunkY; }
    public void setChunkY(int chunkY) { this.chunkY = chunkY; }

    public boolean isEmpty() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                WorldTile tile = tiles[x][y];
                if (tile != null && tile.tileId > 0) return false;
            }
        }
        return true;
    }

    public void clear() {
        for (int x = 0; x < SIZE; x++) {
            for (int y = 0; y < SIZE; y++) {
                tiles[x][y] = new WorldTile();
            }
        }
    }

    @Override
    public String toString() {
        return "Chunk[" + chunkX + "," + chunkY + "]";
    }
}