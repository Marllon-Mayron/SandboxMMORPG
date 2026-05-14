package com.common.sandbox.network.packets;

import com.common.sandbox.model.AttackDefinition;
import com.common.sandbox.network.Packet;

public class AttackRequest extends Packet {
    public String attackerId;
    public float attackerX;
    public float attackerY;
    public float targetX;
    public float targetY;
    public AttackDefinition attackDef;

    public AttackRequest() {}

    public AttackRequest(String attackerId, float attackerX, float attackerY,
                         float targetX, float targetY, AttackDefinition attackDef) {
        this.attackerId = attackerId;
        this.attackerX = attackerX;
        this.attackerY = attackerY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.attackDef = attackDef;
    }
}