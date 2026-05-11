package com.common.sandbox.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class TileTag implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private Map<String, Object> properties;

    public TileTag() {
        this.name = "default";
        this.properties = new HashMap<>();
    }

    public TileTag(String name) {
        this.name = name;
        this.properties = new HashMap<>();
    }

    public TileTag(String name, Map<String, Object> properties) {
        this.name = name;
        this.properties = properties;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Map<String, Object> getProperties() { return properties; }
    public void setProperties(Map<String, Object> properties) { this.properties = properties; }
    public void setProperty(String key, Object value) { properties.put(key, value); }
    public Object getProperty(String key) { return properties.get(key); }

    public boolean isSolid() {
        return "solid".equals(name);
    }

    public boolean isNone() {
        return "none".equals(name) || "default".equals(name);
    }

    public boolean isPassable() {
        return !isSolid();
    }

    public boolean isWater() {
        return "water".equals(name);
    }

    public boolean isLava() {
        return "lava".equals(name);
    }

    public float getWalkSpeed() {
        if (properties.containsKey("walkSpeed")) {
            Object speed = properties.get("walkSpeed");
            if (speed instanceof Number) {
                return ((Number) speed).floatValue();
            }
        }
        return 1.0f; // velocidade normal
    }

    // Tag presets
    public static TileTag water() {
        TileTag tag = new TileTag("water");
        tag.setProperty("liquid", true);
        tag.setProperty("speed", 0.5f);
        return tag;
    }

    public static TileTag solid() {
        TileTag tag = new TileTag("solid");
        tag.setProperty("blocksMovement", true);
        return tag;
    }

    public static TileTag lava() {
        TileTag tag = new TileTag("lava");
        tag.setProperty("liquid", true);
        tag.setProperty("damage", 10);
        return tag;
    }

    public static TileTag grass() {
        TileTag tag = new TileTag("grass");
        tag.setProperty("walkSpeed", 1.0f);
        return tag;
    }

    public static TileTag sand() {
        TileTag tag = new TileTag("sand");
        tag.setProperty("walkSpeed", 0.8f);
        return tag;
    }

    public static TileTag ice() {
        TileTag tag = new TileTag("ice");
        tag.setProperty("walkSpeed", 1.2f);
        tag.setProperty("slippery", true);
        return tag;
    }

    public static TileTag mud() {
        TileTag tag = new TileTag("mud");
        tag.setProperty("walkSpeed", 0.6f);
        return tag;
    }
}