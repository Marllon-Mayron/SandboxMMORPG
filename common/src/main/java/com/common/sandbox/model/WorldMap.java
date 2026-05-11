package com.common.sandbox.model;

import java.util.concurrent.ConcurrentHashMap;

public class WorldMap {
    // Chunks carregados atualmente (server guarda todos, cliente só 9)
    private ConcurrentHashMap<String, Chunk> chunks = new ConcurrentHashMap<>();
    private int viewRadius = 1;  // Carrega raio de 1 chunk (3x3)

    public Chunk getChunk(int chunkX, int chunkY) {
        return chunks.get(key(chunkX, chunkY));
    }

    public void putChunk(Chunk chunk) {
        chunks.put(key(chunk.chunkX, chunk.chunkY), chunk);
    }

    public boolean hasChunk(int chunkX, int chunkY) {
        return chunks.containsKey(key(chunkX, chunkY));
    }

    private String key(int x, int y) { return x + ":" + y; }

    // Obtém chunks ao redor de uma posição global
    public java.util.Map<String, Chunk> getSurroundingChunks(float worldX, float worldY) {
        int centerChunkX = (int) Math.floor(worldX / (Chunk.SIZE * 32));
        int centerChunkY = (int) Math.floor(worldY / (Chunk.SIZE * 32));

        java.util.Map<String, Chunk> result = new ConcurrentHashMap<>();

        for (int dx = -viewRadius; dx <= viewRadius; dx++) {
            for (int dy = -viewRadius; dy <= viewRadius; dy++) {
                int cx = centerChunkX + dx;
                int cy = centerChunkY + dy;
                Chunk chunk = getChunk(cx, cy);
                if (chunk != null) {
                    result.put(key(cx, cy), chunk);
                }
            }
        }
        return result;
    }
}