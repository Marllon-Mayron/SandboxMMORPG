package com.common.sandbox.network.packets.inventory;

import com.common.sandbox.model.item.Inventory;
import com.common.sandbox.network.Packet;

public class InventoryUpdatePacket extends Packet {
    public Inventory inventory;
    public String action; // "UPDATE", "ADD_ITEM", "REMOVE_ITEM", "MOVE_ITEM", "EQUIP", "UNEQUIP"
    public int slot;
    public int targetSlot;
    public String itemId;
    public int quantity;
    public String equipSlot;

    public InventoryUpdatePacket() {}

    public InventoryUpdatePacket(Inventory inventory) {
        this.inventory = inventory;
        this.action = "UPDATE";
    }
}