package com.common.sandbox.model.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Inventory implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = LoggerFactory.getLogger(Inventory.class);
    public static final int TOTAL_SLOTS = 20;
    public static final int MAX_STACK_SIZE = getMaxStackForCategory("common");

    private Map<Integer, ItemStack> slots;
    private Map<String, String> equipped;

    public Inventory() {
        this.slots = new HashMap<>();
        this.equipped = new HashMap<>();
    }

    public Map<Integer, ItemStack> getSlots() { return slots; }
    public void setSlots(Map<Integer, ItemStack> slots) { this.slots = slots; }

    public Map<String, String> getEquipped() { return equipped; }
    public void setEquipped(Map<String, String> equipped) { this.equipped = equipped; }

    /**
     * Retorna o stack máximo baseado na categoria
     */
    public static int getMaxStackForCategory(String category) {
        switch (category) {
            case "weapon":
            case "armor":
            case "equipment":
                return 1;
            case "consumable":
                return 20;
            default:
                return 100;
        }
    }

    /**
     * Adiciona um item ao inventário
     * @return true se adicionou, false se inventário cheio
     */
    public boolean addItem(String itemId, int quantity, ItemDefinition def) {
        int maxStack = getMaxStackForCategory(def.getCategory());
        int remaining = quantity;

        logger.debug("Adding item: {} x{}, maxStack: {}", itemId, quantity, maxStack);

        // Primeiro tenta empilhar em slots existentes
        for (ItemStack stack : slots.values()) {
            if (stack.getItemId().equals(itemId) && stack.getQuantity() < maxStack) {
                int space = maxStack - stack.getQuantity();
                int toAdd = Math.min(remaining, space);
                stack.setQuantity(stack.getQuantity() + toAdd);
                remaining -= toAdd;
                logger.debug("Stacked to existing slot {}: now {}", stack.getSlot(), stack.getQuantity());
                if (remaining <= 0) return true;
            }
        }

        // Depois procura slots vazios
        for (int slot = 0; slot < TOTAL_SLOTS; slot++) {
            if (!slots.containsKey(slot) || slots.get(slot).isEmpty()) {
                int toAdd = Math.min(remaining, maxStack);
                slots.put(slot, new ItemStack(itemId, toAdd, slot));
                remaining -= toAdd;
                logger.debug("Added to new slot {}: {} x{}", slot, itemId, toAdd);
                if (remaining <= 0) return true;
            }
        }

        logger.warn("Failed to add item: inventory full");
        return remaining <= 0;
    }

    /**
     * Remove quantidade de um item
     * @return true se removeu com sucesso
     */
    public boolean removeItem(String itemId, int quantity) {
        int toRemove = quantity;

        for (ItemStack stack : slots.values()) {
            if (stack.getItemId().equals(itemId)) {
                int removeFromStack = Math.min(toRemove, stack.getQuantity());
                stack.setQuantity(stack.getQuantity() - removeFromStack);
                toRemove -= removeFromStack;

                if (stack.getQuantity() <= 0) {
                    slots.remove(stack.getSlot());
                }

                if (toRemove <= 0) return true;
            }
        }

        return toRemove <= 0;
    }

    /**
     * Move item entre slots
     */
    public boolean moveItem(int fromSlot, int toSlot) {
        ItemStack fromStack = slots.get(fromSlot);
        ItemStack toStack = slots.get(toSlot);

        if (fromStack == null || fromStack.isEmpty()) return false;

        // Se destino está vazio, apenas move
        if (toStack == null || toStack.isEmpty()) {
            slots.remove(fromSlot);
            fromStack.setSlot(toSlot);
            slots.put(toSlot, fromStack);
            return true;
        }

        // Se são o mesmo tipo, tenta empilhar
        if (toStack.getItemId().equals(fromStack.getItemId())) {
            // Buscar definição para saber max stack
            // (será resolvido no servidor)
            return false; // Placeholder
        }

        // Troca
        slots.remove(fromSlot);
        slots.remove(toSlot);
        fromStack.setSlot(toSlot);
        toStack.setSlot(fromSlot);
        slots.put(toSlot, fromStack);
        slots.put(fromSlot, toStack);

        return true;
    }

    /**
     * Equipa um item
     */
    public boolean equipItem(int slot, String slotType) {
        ItemStack stack = slots.get(slot);
        if (stack == null || stack.isEmpty()) return false;

        // Remove item equipado atual
        String currentItem = equipped.get(slotType);
        if (currentItem != null && !currentItem.isEmpty()) {
            // Tenta adicionar de volta ao inventário
            // Será tratado no servidor
        }

        equipped.put(slotType, stack.getItemId());
        slots.remove(slot);

        return true;
    }

    /**
     * Desequipa um item
     */
    public boolean unequipItem(String slotType) {
        String itemId = equipped.remove(slotType);
        return itemId != null;
    }

    public ItemStack getSlot(int slot) {
        return slots.getOrDefault(slot, new ItemStack("", 0, slot));
    }
}