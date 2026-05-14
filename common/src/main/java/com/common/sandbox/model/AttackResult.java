package com.common.sandbox.model;

import java.io.Serializable;

public class AttackResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private boolean success;
    private int damage;
    private boolean wasCritical;
    private boolean targetDied;
    private String targetId;
    private String targetName;
    private int targetRemainingHp;
    private String attackTypeName;
    private int attackTypeId;
    private float knockbackX;
    private float knockbackY;

    private float targetX;
    private float targetY;

    public AttackResult() {}

    public AttackResult(boolean success, int damage, boolean wasCritical, boolean targetDied,
                        String targetId, String targetName, int targetRemainingHp,
                        AttackType attackType, float knockbackX, float knockbackY) {
        this.success = success;
        this.damage = damage;
        this.wasCritical = wasCritical;
        this.targetDied = targetDied;
        this.targetId = targetId;
        this.targetName = targetName;
        this.targetRemainingHp = targetRemainingHp;
        this.attackTypeName = attackType != null ? attackType.getName() : "Unknown";
        this.attackTypeId = attackType != null ? attackType.getId() : 1;
        this.knockbackX = knockbackX;
        this.knockbackY = knockbackY;
        this.targetX = 0;
        this.targetY = 0;
    }

    // Construtor com posição do alvo
    public AttackResult(boolean success, int damage, boolean wasCritical, boolean targetDied,
                        String targetId, String targetName, int targetRemainingHp,
                        AttackType attackType, float knockbackX, float knockbackY,
                        float targetX, float targetY) {
        this(success, damage, wasCritical, targetDied, targetId, targetName, targetRemainingHp,
                attackType, knockbackX, knockbackY);
        this.targetX = targetX;
        this.targetY = targetY;
    }

    // Getters existentes
    public boolean isSuccess() { return success; }
    public int getDamage() { return damage; }
    public boolean isWasCritical() { return wasCritical; }
    public boolean isTargetDied() { return targetDied; }
    public String getTargetId() { return targetId; }
    public String getTargetName() { return targetName; }
    public int getTargetRemainingHp() { return targetRemainingHp; }
    public String getAttackTypeName() { return attackTypeName; }
    public int getAttackTypeId() { return attackTypeId; }
    public float getKnockbackX() { return knockbackX; }
    public float getKnockbackY() { return knockbackY; }

    // NOVOS GETTERS
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }

    // Setters
    public void setSuccess(boolean success) { this.success = success; }
    public void setDamage(int damage) { this.damage = damage; }
    public void setWasCritical(boolean wasCritical) { this.wasCritical = wasCritical; }
    public void setTargetDied(boolean targetDied) { this.targetDied = targetDied; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public void setTargetRemainingHp(int targetRemainingHp) { this.targetRemainingHp = targetRemainingHp; }
    public void setAttackTypeName(String attackTypeName) { this.attackTypeName = attackTypeName; }
    public void setAttackTypeId(int attackTypeId) { this.attackTypeId = attackTypeId; }
    public void setKnockbackX(float knockbackX) { this.knockbackX = knockbackX; }
    public void setKnockbackY(float knockbackY) { this.knockbackY = knockbackY; }
    public void setTargetX(float targetX) { this.targetX = targetX; }
    public void setTargetY(float targetY) { this.targetY = targetY; }

    public AttackType getAttackType() {
        return AttackType.fromId(attackTypeId);
    }

    @Override
    public String toString() {
        return String.format("AttackResult{success=%s, damage=%d, critical=%s, target=%s}",
                success, damage, wasCritical, targetName);
    }
}