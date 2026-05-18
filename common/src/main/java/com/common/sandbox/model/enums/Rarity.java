package com.common.sandbox.model.enums;

import java.io.Serializable;
import java.awt.Color;

/**
 * Níveis de raridade de itens no jogo
 */
public enum Rarity implements Serializable {

    COMMON("common", "Comum", 0, new Color(180, 180, 180)),
    UNCOMMON("uncommon", "Incomum", 1, new Color(80, 200, 80)),
    RARE("rare", "Raro", 2, new Color(80, 120, 255)),
    EPIC("epic", "Épico", 3, new Color(160, 80, 200)),
    LEGENDARY("legendary", "Lendário", 4, new Color(255, 160, 50)),
    MYTHIC("mythic", "Mítico", 5, new Color(210, 50, 210));

    private final String id;
    private final String displayName;
    private final int tier;
    private final Color color;

    Rarity(String id, String displayName, int tier, Color color) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.color = color;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public int getTier() { return tier; }
    public Color getColor() { return color; }

    public String getHexColor() {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    public static Rarity fromId(String id) {
        for (Rarity rarity : values()) {
            if (rarity.id.equals(id)) {
                return rarity;
            }
        }
        return COMMON;
    }

    public static Rarity fromTier(int tier) {
        for (Rarity rarity : values()) {
            if (rarity.tier == tier) {
                return rarity;
            }
        }
        return COMMON;
    }
}