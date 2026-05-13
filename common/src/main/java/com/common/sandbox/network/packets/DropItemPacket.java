package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class DropItemPacket extends Packet {
    public int slot;
    public int quantity;
    public String playerId;

    public DropItemPacket() {}

    public DropItemPacket(int slot, int quantity, String playerId) {
        this.slot = slot;
        this.quantity = quantity;
        this.playerId = playerId;
    }
}