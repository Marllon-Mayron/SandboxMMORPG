package com.common.sandbox.network.packets;

import com.common.sandbox.model.AttackType;
import com.common.sandbox.network.Packet;

public class AttackRequest extends Packet {
    public String attackerId;
    public float targetX;
    public float targetY;
    public AttackType attackType;

    public AttackRequest() {}

    public AttackRequest(String attackerId, float targetX, float targetY, AttackType attackType) {
        this.attackerId = attackerId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.attackType = attackType;
    }
}