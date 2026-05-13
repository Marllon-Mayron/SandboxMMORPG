package com.common.sandbox.network.packets;

import com.common.sandbox.model.AttackResult;
import com.common.sandbox.network.Packet;

public class AttackBroadcast extends Packet {
    public String attackerId;
    public String attackerName;
    public float attackerX;
    public float attackerY;
    public AttackResult result;

    public AttackBroadcast() {}

    public AttackBroadcast(String attackerId, String attackerName, float attackerX, float attackerY, AttackResult result) {
        this.attackerId = attackerId;
        this.attackerName = attackerName;
        this.attackerX = attackerX;
        this.attackerY = attackerY;
        this.result = result;
    }

    // Getters e Setters para facilitar acesso
    public String getAttackerId() { return attackerId; }
    public void setAttackerId(String attackerId) { this.attackerId = attackerId; }
    public String getAttackerName() { return attackerName; }
    public void setAttackerName(String attackerName) { this.attackerName = attackerName; }
    public float getAttackerX() { return attackerX; }
    public void setAttackerX(float attackerX) { this.attackerX = attackerX; }
    public float getAttackerY() { return attackerY; }
    public void setAttackerY(float attackerY) { this.attackerY = attackerY; }
    public AttackResult getResult() { return result; }
    public void setResult(AttackResult result) { this.result = result; }
}