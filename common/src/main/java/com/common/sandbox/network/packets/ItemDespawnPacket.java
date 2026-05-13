package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class ItemDespawnPacket extends Packet {
    public String instanceId;

    public ItemDespawnPacket() {}
    public ItemDespawnPacket(String instanceId) {
        this.instanceId = instanceId;
    }
}