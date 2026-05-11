package com.common.sandbox.model;

import java.io.Serializable;

public class WorldTile implements Serializable {
    private static final long serialVersionUID = 1L;

    public String spritesheetPath;
    public int tileId;
    public boolean solid;  // ← manter para compatibilidade, mas NÃO usar para colisão
    public String tag;

    public WorldTile() {
        this.spritesheetPath = "";
        this.tileId = -1;
        this.solid = false;
        this.tag = "default";
    }

    public WorldTile(String spritesheetPath, int tileId, boolean solid) {
        this(spritesheetPath, tileId, solid, "default");
    }

    public WorldTile(String spritesheetPath, int tileId, boolean solid, String tag) {
        this.spritesheetPath = spritesheetPath != null ? spritesheetPath : "";
        this.tileId = tileId;
        this.solid = solid;
        this.tag = tag != null ? tag : "default";
    }

    public boolean isEmpty() {
        return tileId < 0 || spritesheetPath == null || spritesheetPath.isEmpty();
    }

    public boolean isSolid() {
        return "solid".equals(tag);
    }

    public boolean isPassable() {
        return !"solid".equals(tag);
    }

    public boolean isWater() {
        return "water".equals(tag);
    }

    public boolean isLava() {
        return "lava".equals(tag);
    }

    public float getWalkSpeed() {
        switch (tag) {
            case "water": return 0.5f;
            case "lava": return 0.4f;
            case "sand": return 0.8f;
            case "ice": return 1.2f;
            case "mud": return 0.6f;
            case "grass": return 1.0f;
            default: return 1.0f;
        }
    }

    public int getDamage() {
        switch (tag) {
            case "lava": return 10;
            default: return 0;
        }
    }

    @Override
    public String toString() {
        if (isEmpty()) return "WorldTile[EMPTY]";
        return "WorldTile[path=" + spritesheetPath + ", id=" + tileId + ", tag=" + tag + "]";
    }
}