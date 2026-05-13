package com.common.sandbox.network.packets;

import com.common.sandbox.model.GroundItem;
import com.common.sandbox.network.Packet;

public class ItemSpawnPacket extends Packet {
    public GroundItem item;

    public ItemSpawnPacket() {}
    public ItemSpawnPacket(GroundItem item) {
        this.item = item;
    }
}