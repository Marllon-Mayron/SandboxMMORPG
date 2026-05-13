package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;
import com.common.sandbox.model.Player;

public class MovementRequest extends Packet {
    public String playerId;
    public float x, y;
    public String direction;

    // Status completos do jogador
    public int currentHp;
    public int currentMana;
    public int currentStamina;
    public int currentGold;
    public int currentExperience;
    public int currentLevel;

    public MovementRequest() {}

    public MovementRequest(String playerId, float x, float y, String direction) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }

    // Construtor completo com todos os status
    public MovementRequest(Player player, float x, float y, String direction) {
        this.playerId = player.getId();
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.currentHp = player.getCurrentHp();
        this.currentMana = player.getCurrentMana();
        this.currentStamina = player.getCurrentStamina();
        this.currentGold = player.getGold();
        this.currentExperience = player.getExperience();
        this.currentLevel = player.getLevel();
    }
}