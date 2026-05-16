package com.common.sandbox.network.packets.inventory;

import com.common.sandbox.network.Packet;

public class PickupResultPacket extends Packet {
    public boolean success;
    public String itemName;
    public int quantity;

    public PickupResultPacket() {}

    public PickupResultPacket(boolean success, String itemName, int quantity) {
        this.success = success;
        this.itemName = itemName;
        this.quantity = quantity;
    }
}