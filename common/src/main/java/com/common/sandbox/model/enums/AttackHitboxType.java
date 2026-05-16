package com.common.sandbox.model.enums;

import java.io.Serializable;

public enum AttackHitboxType implements Serializable {
    CIRCLE(0, "Circle", "Círculo"),
    RECTANGLE(1, "Rectangle", "Retângulo"),
    CONE(2, "Cone", "Cone"),
    LINE(3, "Line", "Linha");

    private final int id;
    private final String code;
    private final String name;

    AttackHitboxType(int id, String code, String name) {
        this.id = id;
        this.code = code;
        this.name = name;
    }

    public int getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }

    public static AttackHitboxType fromId(int id) {
        for (AttackHitboxType type : values()) {
            if (type.id == id) return type;
        }
        return RECTANGLE;
    }
}