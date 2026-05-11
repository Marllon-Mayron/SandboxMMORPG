package com.common.sandbox.network.packets;

import com.common.sandbox.model.Chunk;
import com.common.sandbox.network.Packet;

public class ChunkUpdatePacket extends Packet {
    public int chunkX;
    public int chunkY;
    public Chunk chunkData;
    public boolean isFullUpdate = true;

    public ChunkUpdatePacket() {}

    public ChunkUpdatePacket(int chunkX, int chunkY) {
        this.chunkX = chunkX;
        this.chunkY = chunkY;
    }

    public ChunkUpdatePacket(Chunk chunk) {
        this.chunkX = chunk.chunkX;
        this.chunkY = chunk.chunkY;
        this.chunkData = chunk;
    }
}