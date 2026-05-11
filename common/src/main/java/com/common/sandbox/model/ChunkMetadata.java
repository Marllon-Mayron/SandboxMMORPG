package com.common.sandbox.model;

import java.io.Serializable;

public class ChunkMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    private int chunkX;
    private int chunkY;
    private long lastModified;
    private String modifiedBy;

    public ChunkMetadata() {
        this.lastModified = System.currentTimeMillis();
        this.modifiedBy = "unknown";
    }

    public ChunkMetadata(int chunkX, int chunkY) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
        this.lastModified = System.currentTimeMillis();
        this.modifiedBy = "unknown";
    }

    public int getChunkX() { return chunkX; }
    public void setChunkX(int chunkX) { this.chunkX = chunkX; }

    public int getChunkY() { return chunkY; }
    public void setChunkY(int chunkY) { this.chunkY = chunkY; }

    public long getLastModified() { return lastModified; }
    public void setLastModified(long lastModified) { this.lastModified = lastModified; }

    public String getModifiedBy() { return modifiedBy; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}