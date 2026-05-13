package com.common.sandbox.model;

import java.io.Serializable;

public enum AttackType implements Serializable {
    MELEE_SWORD(1, "Sword Slash", 1.5f, 1.0f),      // raio, duração animação, multiplicador dano
    MELEE_DAGGER(2, "Dagger Stab", 1.2f, 0.8f),
    MELEE_AXE(3, "Axe Chop", 1.8f, 1.3f),
    RANGED_BOW(4, "Arrow Shot", 8.0f, 1.2f),
    RANGED_MAGIC(5, "Magic Missile", 10.0f, 1.5f),
    SPECIAL_WHIRLWIND(6, "Whirlwind", 2.5f, 0.7f);

    private final int id;
    private final String name;
    private final float range;
    private final float damageMultiplier;

    AttackType(int id, String name, float range, float damageMultiplier) {
        this.id = id;
        this.name = name;
        this.range = range;
        this.damageMultiplier = damageMultiplier;
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public float getRange() { return range; }
    public float getDamageMultiplier() { return damageMultiplier; }

    public static AttackType fromId(int id) {
        for (AttackType type : values()) {
            if (type.id == id) return type;
        }
        return MELEE_SWORD;
    }
}