package com.common.sandbox.model;

import java.io.Serializable;

public class AttackDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private AttackHitboxType hitboxType;
    private float range;
    private float width;
    private float height;
    private float radius;
    private float damageMultiplier;
    private float cooldownSeconds;
    private float manaCost;
    private float staminaCost;
    private String animationId;
    private String projectileId;
    private boolean isRanged;
    private int maxTargets;
    private float knockbackPower;

    private String attackAnimation;    // "sword_slash", "bow_shoot", etc.
    private float projectileSpeed;
    private float hitboxDuration = 0.25f;

    public AttackDefinition() {}

    // Getters e Setters existentes...
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public AttackHitboxType getHitboxType() { return hitboxType; }
    public void setHitboxType(AttackHitboxType hitboxType) { this.hitboxType = hitboxType; }
    public float getRange() { return range; }
    public void setRange(float range) { this.range = range; }
    public float getWidth() { return width; }
    public void setWidth(float width) { this.width = width; }
    public float getHeight() { return height; }
    public void setHeight(float height) { this.height = height; }
    public float getRadius() { return radius; }
    public void setRadius(float radius) { this.radius = radius; }
    public float getDamageMultiplier() { return damageMultiplier; }
    public void setDamageMultiplier(float damageMultiplier) { this.damageMultiplier = damageMultiplier; }
    public float getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(float cooldownSeconds) { this.cooldownSeconds = cooldownSeconds; }
    public float getManaCost() { return manaCost; }
    public void setManaCost(float manaCost) { this.manaCost = manaCost; }
    public float getStaminaCost() { return staminaCost; }
    public void setStaminaCost(float staminaCost) { this.staminaCost = staminaCost; }
    public String getAnimationId() { return animationId; }
    public void setAnimationId(String animationId) { this.animationId = animationId; }
    public String getProjectileId() { return projectileId; }
    public void setProjectileId(String projectileId) { this.projectileId = projectileId; }
    public boolean isRanged() { return isRanged; }
    public void setRanged(boolean ranged) { isRanged = ranged; }
    public int getMaxTargets() { return maxTargets; }
    public void setMaxTargets(int maxTargets) { this.maxTargets = maxTargets; }
    public float getKnockbackPower() { return knockbackPower; }
    public void setKnockbackPower(float knockbackPower) { this.knockbackPower = knockbackPower; }

    public String getAttackAnimation() { return attackAnimation; }
    public void setAttackAnimation(String attackAnimation) { this.attackAnimation = attackAnimation; }
    public float getProjectileSpeed() { return projectileSpeed; }
    public void setProjectileSpeed(float projectileSpeed) { this.projectileSpeed = projectileSpeed; }
    public float getHitboxDuration() { return hitboxDuration; }
    public void setHitboxDuration(float hitboxDuration) { this.hitboxDuration = hitboxDuration; }

    // Factory methods
    public static AttackDefinition createMeleeSword() {
        AttackDefinition def = new AttackDefinition();
        def.setId("melee_sword");
        def.setName("Corte com Espada");
        def.setHitboxType(AttackHitboxType.RECTANGLE);
        def.setRange(64f);
        def.setWidth(48f);
        def.setHeight(32f);
        def.setDamageMultiplier(1.2f);
        def.setCooldownSeconds(1.0f);
        def.setStaminaCost(1f);
        def.setMaxTargets(3);
        def.setKnockbackPower(30f);
        def.setRanged(false);
        def.setHitboxDuration(0.3f);
        def.setAttackAnimation("sword_slash");
        return def;
    }

    public static AttackDefinition createMeleeDagger() {
        AttackDefinition def = new AttackDefinition();
        def.setId("melee_dagger");
        def.setName("Adaga Rápida");
        def.setHitboxType(AttackHitboxType.RECTANGLE);
        def.setRange(48f);
        def.setWidth(36f);
        def.setHeight(28f);
        def.setDamageMultiplier(0.7f);
        def.setCooldownSeconds(0.5f);
        def.setStaminaCost(3f);
        def.setMaxTargets(1);
        def.setKnockbackPower(10f);
        def.setRanged(false);
        def.setAttackAnimation("dagger_stab");
        return def;
    }

    public static AttackDefinition createRangedBow() {
        AttackDefinition def = new AttackDefinition();
        def.setId("ranged_bow");
        def.setName("Flecha");
        def.setHitboxType(AttackHitboxType.CIRCLE);
        def.setRange(400f);
        def.setRadius(24f);
        def.setDamageMultiplier(1.2f);
        def.setCooldownSeconds(1.5f);
        def.setStaminaCost(10f);
        def.setMaxTargets(1);
        def.setKnockbackPower(15f);
        def.setRanged(true);
        def.setProjectileId("arrow");
        def.setAttackAnimation("bow_shoot");
        def.setProjectileSpeed(600f);
        return def;
    }
}