package com.common.sandbox.model.player;

import java.io.Serializable;

public class CombatStats implements Serializable {
    private static final long serialVersionUID = 1L;

    // Atributos base de combate
    private int baseDamage = 5;
    private int criticalChance = 10;  // percentual (0-100)
    private int criticalDamage = 150;  // percentual (150% = 1.5x)
    private float attackSpeed = 1.0f;   // ataques por segundo
    private float attackRange = 1.5f;   // range em tiles (~96 pixels)

    // Modificadores de equipamento (calculados dinamicamente)
    private transient int weaponDamageBonus = 0;
    private transient int strengthBonus = 0;

    public CombatStats() {}

    public int calculateDamage() {
        int totalDamage = baseDamage + weaponDamageBonus + strengthBonus;
        return Math.max(1, totalDamage);
    }

    public int calculateDamageWithCritical() {
        int damage = calculateDamage();
        if (isCriticalHit()) {
            damage = damage * criticalDamage / 100;
        }
        return damage;
    }

    public boolean isCriticalHit() {
        return (int)(Math.random() * 100) < criticalChance;
    }

    public int getBaseDamage() { return baseDamage; }
    public void setBaseDamage(int baseDamage) { this.baseDamage = baseDamage; }
    public int getCriticalChance() { return criticalChance; }
    public void setCriticalChance(int criticalChance) { this.criticalChance = criticalChance; }
    public int getCriticalDamage() { return criticalDamage; }
    public void setCriticalDamage(int criticalDamage) { this.criticalDamage = criticalDamage; }
    public float getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(float attackSpeed) { this.attackSpeed = attackSpeed; }
    public float getAttackRange() { return attackRange; }
    public void setAttackRange(float attackRange) { this.attackRange = attackRange; }
    public int getWeaponDamageBonus() { return weaponDamageBonus; }
    public void setWeaponDamageBonus(int weaponDamageBonus) { this.weaponDamageBonus = weaponDamageBonus; }
    public int getStrengthBonus() { return strengthBonus; }
    public void setStrengthBonus(int strengthBonus) { this.strengthBonus = strengthBonus; }
}