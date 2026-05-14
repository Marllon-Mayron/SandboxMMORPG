package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;
import com.common.sandbox.model.Player;

public class PlayerStatePacket extends Packet {
    public String playerId;
    public String username;
    public float x, y;
    public String direction;

    public int currentHp;
    public int currentMana;
    public int currentStamina;

    public int baseHp;
    public int baseMana;
    public int baseStamina;

    public int strength;
    public int agility;
    public int wisdom;

    public int level;
    public int gold;
    public int experience;

    public float currentAttackCooldown;

    // Indica se é uma atualização completa (para sincronização inicial)
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

        this.baseHp = player.getBaseHp();
        this.baseMana = player.getBaseMana();
        this.baseStamina = player.getBaseStamina();

        this.strength = player.getStrength();
        this.agility = player.getAgility();
        this.wisdom = player.getWisdom();

        this.level = player.getLevel();
        this.gold = player.getGold();
        this.experience = player.getExperience();

        this.currentAttackCooldown = player.getCurrentAttackCooldown();
    }

    public int getMaxHp() {
        return baseHp + (level - 1) * 10 + strength * 5;
    }

    public int getMaxMana() {
        return baseMana + (level - 1) * 5 + wisdom * 5;
    }

    public int getMaxStamina() {
        return baseStamina + (level - 1) * 5 + agility * 5;
    }

    @Override
    public String toString() {
        return String.format("PlayerState{id=%s, name=%s, pos=(%.1f,%.1f), hp=%d/%d, lvl=%d, str=%d, cooldown=%.2f}",
                playerId, username, x, y, currentHp, getMaxHp(), level, strength, currentAttackCooldown);
    }
}