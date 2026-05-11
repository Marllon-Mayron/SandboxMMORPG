package com.sandbox.client.editor.models;

import com.badlogic.gdx.graphics.Color;

public enum LayerType {
    GROUND(0, "Ground", new Color(0.3f, 0.8f, 0.3f, 0.3f)),
    DECORATION(1, "Decoration", new Color(0.8f, 0.8f, 0.3f, 0.3f)),
    CEILING(2, "Ceiling", new Color(0.8f, 0.3f, 0.8f, 0.3f));

    public final int id;
    public final String name;
    public final Color color;

    LayerType(int id, String name, Color color) {
        this.id = id;
        this.name = name;
        this.color = color;
    }

    public static LayerType fromId(int id) {
        for (LayerType t : values()) {
            if (t.id == id) return t;
        }
        return GROUND;
    }

    public static LayerType fromName(String name) {
        for (LayerType t : values()) {
            if (t.name.equals(name)) return t;
        }
        return GROUND;
    }
}