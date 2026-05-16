package com.common.sandbox.network.packets.combat;

import com.common.sandbox.model.combat.AttackDefinition;
import com.common.sandbox.model.combat.AttackResult;
import com.common.sandbox.network.Packet;
import java.util.List;

public class AttackBroadcast extends Packet {
    public String attackerId;
    public String attackerName;
    public float attackerX;
    public float attackerY;
    public float targetX;
    public float targetY;
    public AttackDefinition attackDef;  // Definição completa do ataque
    public List<AttackResult> results;   // Múltiplos resultados (para ataques em área)
    public long timestamp;

    public AttackBroadcast() {}

    // Construtor para ataques simples (um alvo)
    public AttackBroadcast(String attackerId, String attackerName,
                           float attackerX, float attackerY,
                           float targetX, float targetY,
                           AttackDefinition attackDef,
                           AttackResult result) {
        this.attackerId = attackerId;
        this.attackerName = attackerName;
        this.attackerX = attackerX;
        this.attackerY = attackerY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.attackDef = attackDef;
        this.results = java.util.Collections.singletonList(result);
        this.timestamp = System.currentTimeMillis();
    }

    // Construtor para ataques em área (múltiplos alvos)
    public AttackBroadcast(String attackerId, String attackerName,
                           float attackerX, float attackerY,
                           float targetX, float targetY,
                           AttackDefinition attackDef,
                           List<AttackResult> results) {
        this.attackerId = attackerId;
        this.attackerName = attackerName;
        this.attackerX = attackerX;
        this.attackerY = attackerY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.attackDef = attackDef;
        this.results = results;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters (mantendo compatibilidade com código existente)
    public String getAttackerId() { return attackerId; }
    public String getAttackerName() { return attackerName; }
    public float getAttackerX() { return attackerX; }
    public float getAttackerY() { return attackerY; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public AttackDefinition getAttackDef() { return attackDef; }
    public List<AttackResult> getResults() { return results; }
    public long getTimestamp() { return timestamp; }

    // Método helper para pegar o primeiro resultado (ataque simples)
    public AttackResult getResult() {
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }
}