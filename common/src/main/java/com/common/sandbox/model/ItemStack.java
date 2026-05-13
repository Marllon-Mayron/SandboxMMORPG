package com.common.sandbox.model;

import java.io.Serializable;

public class ItemStack implements Serializable {
    private static final long serialVersionUID = 1L;

    private String itemId;
    private int quantity;
    private int slot;

    public ItemStack() {}

    public ItemStack(String itemId, int quantity, int slot) {
        this.itemId = itemId;
        this.quantity = quantity;
        this.slot = slot;
    }

    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getSlot() { return slot; }
    public void setSlot(int slot) { this.slot = slot; }

    public boolean isEmpty() { return itemId == null || itemId.isEmpty() || quantity <= 0; }

    @Override
    public String toString() {
        return "ItemStack{itemId='" + itemId + "', qty=" + quantity + ", slot=" + slot + "}";
    }
}