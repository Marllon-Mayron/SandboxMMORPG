package com.common.sandbox.network.packets;

import com.common.sandbox.model.Chunk;

public class ChunkDataPacket extends com.common.sandbox.network.Packet {
    public int chunkX, chunkY;
    public Chunk chunk;

    public ChunkDataPacket() {}

    public ChunkDataPacket(Chunk chunk) {
        this.chunkX = chunk.chunkX;
        this.chunkY = chunk.chunkY;
        this.chunk = chunk;
    }
}
