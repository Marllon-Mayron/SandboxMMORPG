package com.common.sandbox.network.packets.combat;

import com.common.sandbox.model.enums.AttackType;
import com.common.sandbox.network.Packet;

public class DamagePacket extends Packet {
    public String targetId;
    public int damage;
    public boolean wasCritical;
    public int newHp;
    public String attackTypeName;
    public int attackTypeId;

    public DamagePacket() {}

    public DamagePacket(String targetId, int damage, boolean wasCritical, int newHp, AttackType attackType) {
        this.targetId = targetId;
        this.damage = damage;
        this.wasCritical = wasCritical;
        this.newHp = newHp;
        this.attackTypeName = attackType != null ? attackType.getName() : "Unknown";
        this.attackTypeId = attackType != null ? attackType.getId() : 1;
    }

    // Getters e Setters
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }
    public boolean isWasCritical() { return wasCritical; }
    public void setWasCritical(boolean wasCritical) { this.wasCritical = wasCritical; }
    public int getNewHp() { return newHp; }
    public void setNewHp(int newHp) { this.newHp = newHp; }
    public String getAttackTypeName() { return attackTypeName; }
    public void setAttackTypeName(String attackTypeName) { this.attackTypeName = attackTypeName; }
    public int getAttackTypeId() { return attackTypeId; }
    public void setAttackTypeId(int attackTypeId) { this.attackTypeId = attackTypeId; }

    public AttackType getAttackType() {
        return AttackType.fromId(attackTypeId);
    }
}