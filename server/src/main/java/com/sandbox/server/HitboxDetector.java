package com.sandbox.server;

import com.common.sandbox.model.AttackDefinition;
import com.common.sandbox.model.AttackHitboxType;
import com.common.sandbox.model.Player;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class HitboxDetector {
    private static final Logger logger = LoggerFactory.getLogger(HitboxDetector.class);
    /**
     * Detecta quais jogadores estão dentro da hitbox do ataque
     */
    public static List<Player> getPlayersInHitbox(Player attacker, AttackDefinition attack,
                                                  float targetX, float targetY,
                                                  Collection<Player> allPlayers) {
        List<Player> hitPlayers = new ArrayList<>();

        float attackerX = attacker.getX();
        float attackerY = attacker.getY();
        float range = attack.getRange();

        logger.info("=== HITBOX CHECK ===");
        logger.info("Attacker: ({}, {})", attackerX, attackerY);
        logger.info("Range: {}", range);

        for (Player target : allPlayers) {
            if (target.getId().equals(attacker.getId())) continue;
            if (target.getCurrentHp() <= 0) continue;

            float targetXpos = target.getX();
            float targetYpos = target.getY();

            float dx = targetXpos - attackerX;
            float dy = targetYpos - attackerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            logger.info("Checking {} at ({}, {}) - Distance: {}",
                    target.getUsername(), targetXpos, targetYpos, distance);

            if (distance <= range) {
                logger.info("  ✓ IN RANGE! Adding to hit list");
                hitPlayers.add(target);
            } else {
                logger.info("  ✗ OUT OF RANGE");
            }
        }

        // Limitar número de alvos
        if (hitPlayers.size() > attack.getMaxTargets()) {
            hitPlayers.sort((a, b) -> {
                float da = (float) Math.hypot(a.getX() - attackerX, a.getY() - attackerY);
                float db = (float) Math.hypot(b.getX() - attackerX, b.getY() - attackerY);
                return Float.compare(da, db);
            });
            hitPlayers = hitPlayers.subList(0, attack.getMaxTargets());
        }

        logger.info("Total targets hit: {}", hitPlayers.size());
        return hitPlayers;
    }

    private static boolean isPointInHitbox(float originX, float originY,
                                           float dirX, float dirY,
                                           float pointX, float pointY,
                                           AttackDefinition attack) {
        float toPointX = pointX - originX;
        float toPointY = pointY - originY;

        // Projetar o ponto na direção do ataque
        float dot = toPointX * dirX + toPointY * dirY;

        // Se está atrás do jogador, ignorar
        if (dot < 0) return false;

        // Se está além do range, ignorar
        if (dot > attack.getRange()) return false;

        // Calcular distância perpendicular
        float perpX = toPointX - dot * dirX;
        float perpY = toPointY - dot * dirY;
        float perpDist = (float) Math.sqrt(perpX * perpX + perpY * perpY);

        switch (attack.getHitboxType()) {
            case CIRCLE:
                return perpDist <= attack.getRadius();

            case RECTANGLE:
                float halfWidth = attack.getWidth() / 2;
                float halfHeight = attack.getHeight() / 2;

                // Para retângulo, verificar se está dentro das dimensões
                // A largura é perpendicular, altura é na direção do ataque
                return Math.abs(perpX) <= halfWidth &&
                        Math.abs(perpY) <= halfHeight &&
                        dot >= 0 && dot <= attack.getRange();

            case CONE:
                // Ângulo do cone (em radianos)
                float coneAngle = (float) Math.toRadians(60); // 60 graus
                float angleToPoint = (float) Math.atan2(perpDist, dot);
                return angleToPoint <= coneAngle / 2;

            case LINE:
                // Linha fina (como um raio)
                return perpDist <= 8f && dot >= 0 && dot <= attack.getRange();

            default:
                return perpDist <= 32f;
        }
    }

    /**
     * Calcula a posição do projétil para ataques à distância
     */
    public static float[] getProjectileStartPosition(float originX, float originY,
                                                     float dirX, float dirY,
                                                     float offsetDistance) {
        float startX = originX + dirX * offsetDistance;
        float startY = originY + dirY * offsetDistance;
        return new float[]{startX, startY};
    }
}