package com.common.sandbox.network.packets.player;

import com.common.sandbox.network.Packet;
import com.common.sandbox.model.player.Player;

public class PlayerStatePacket extends Packet {
    public String playerId;
    public String username;
    public float x, y;
    public String direction;

    public int currentHp;
    public int currentMana;
    public int currentStamina;

    public int maxHp;
    public int maxMana;
    public int maxStamina;

    public int level;
    public int gold;
    public int experience;
    public int attributePoints;

    // Poderes
    public int physicalPower;
    public int rangedPower;
    public int magicPower;

    // Defesas
    public int physicalDefense;
    public int magicDefense;

    // Chance e multiplicadores
    public float criticalChance;
    public float criticalDamage;
    public float dodgeChance;

    // Velocidades
    public float attackSpeed;
    public float movementSpeed;

    // Utilidades
    public float cooldownReduction;
    public float lifeSteal;
    public float manaSteal;
    public float tenacity;

    // Regeneracao
    public int hpRegen;
    public int manaRegen;
    public int staminaRegen;

    public float currentAttackCooldown;
    public boolean fullSync = false;

    public PlayerStatePacket() {}

    public PlayerStatePacket(Player player) {
        this.playerId = player.getId();
        this.username = player.getUsername();
        this.x = player.getX();
        this.y = player.getY();
        this.direction = player.getDirection();

        this.currentHp = player.getCurrentHp();
        this.currentMana = player.getCurrentMana();
        this.currentStamina = player.getCurrentStamina();

        this.maxHp = player.getMaxHp();
        this.maxMana = player.getMaxMana();
        this.maxStamina = player.getMaxStamina();

        this.level = player.getLevel();
        this.gold = player.getGold();
        this.experience = player.getExperience();
        this.attributePoints = player.getAttributePoints();

        this.physicalPower = player.getPhysicalPower();
        this.rangedPower = player.getRangedPower();
        this.magicPower = player.getMagicPower();

        this.physicalDefense = player.getPhysicalDefense();
        this.magicDefense = player.getMagicDefense();

        this.criticalChance = player.getCriticalChance();
        this.criticalDamage = player.getCriticalDamage();
        this.dodgeChance = player.getDodgeChance();

        this.attackSpeed = player.getAttackSpeed();
        this.movementSpeed = player.getMovementSpeed();

        this.cooldownReduction = player.getCooldownReduction();
        this.lifeSteal = player.getLifeSteal();
        this.manaSteal = player.getManaSteal();
        this.tenacity = player.getTenacity();

        this.hpRegen = player.getHpRegen();
        this.manaRegen = player.getManaRegen();
        this.staminaRegen = player.getStaminaRegen();

        this.currentAttackCooldown = player.getCurrentAttackCooldown();
    }

    @Override
    public String toString() {
        return String.format("PlayerState{id=%s, name=%s, pos=(%.1f,%.1f), hp=%d/%d, lvl=%d, ap=%d}",
                playerId, username, x, y, currentHp, maxHp, level, attributePoints);
    }
}