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
    }

    // Getters
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

    // Métodos helpers
    public AttackType getAttackType() {
        return AttackType.fromId(attackTypeId);
    }

    @Override
    public String toString() {
        return String.format("AttackResult{success=%s, damage=%d, critical=%s, target=%s}",
                success, damage, wasCritical, targetName);
    }
}