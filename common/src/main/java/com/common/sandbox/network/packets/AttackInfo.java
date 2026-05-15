package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;
import java.io.Serializable;

public class AttackInfo extends Packet implements Serializable {
    public String attackerId;
    public String attackerName;
    public float attackerX;
    public float attackerY;
    public String attackId;           // ID do ataque (ex: "melee_sword")
    public float targetX;             // Posição do mouse (onde o ataque vai)
    public float targetY;
    public long timestamp;

    public AttackInfo() {}

    public AttackInfo(String attackerId, String attackerName,
                      float attackerX, float attackerY,
                      String attackId, float targetX, float targetY) {
        this.attackerId = attackerId;
        this.attackerName = attackerName;
        this.attackerX = attackerX;
        this.attackerY = attackerY;
        this.attackId = attackId;
        this.targetX = targetX;
        this.targetY = targetY;
        this.timestamp = System.currentTimeMillis();
    }
}