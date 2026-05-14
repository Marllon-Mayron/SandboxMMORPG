package com.sandbox.client.renderer.effects;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.common.sandbox.model.AttackResult;
import com.common.sandbox.network.packets.AttackBroadcast;
import java.util.concurrent.ConcurrentLinkedQueue;

public class AttackEffectManager {
    private final ConcurrentLinkedQueue<AttackEffect> effects = new ConcurrentLinkedQueue<>();

    public void addAttackEffect(AttackBroadcast broadcast) {
        if (broadcast == null || broadcast.attackDef == null) return;

        float duration = 0.2f;
        AttackEffect.EffectType effectType;
        String projectileType = "arrow";

        String animation = broadcast.attackDef.getAttackAnimation();
        if (animation == null) animation = "sword_slash";

        switch (animation) {
            case "sword_slash":
                effectType = AttackEffect.EffectType.SWORD_SLASH;
                duration = 0.15f;
                break;
            case "dagger_stab":
                effectType = AttackEffect.EffectType.DAGGER_STAB;
                duration = 0.1f;
                break;
            case "bow_shoot":
                effectType = AttackEffect.EffectType.BOW_SHOOT;
                duration = 0.2f;
                projectileType = "arrow";
                break;
            case "fireball":
                effectType = AttackEffect.EffectType.FIREBALL;
                duration = 0.3f;
                projectileType = "fireball";
                break;
            default:
                effectType = AttackEffect.EffectType.SWORD_SLASH;
        }

        // Para ataques corpo a corpo
        if (!broadcast.attackDef.isRanged() && broadcast.results != null && !broadcast.results.isEmpty()) {
            AttackResult firstResult = broadcast.results.get(0);
            float targetX = firstResult.getTargetX() != 0 ? firstResult.getTargetX() : broadcast.targetX;
            float targetY = firstResult.getTargetY() != 0 ? firstResult.getTargetY() : broadcast.targetY;

            if (targetX == 0 && targetY == 0) {
                targetX = broadcast.attackerX + firstResult.getKnockbackX();
                targetY = broadcast.attackerY + firstResult.getKnockbackY();
            }

            AttackEffect effect = new AttackEffect(
                    effectType,
                    broadcast.attackerX,
                    broadcast.attackerY,
                    targetX,
                    targetY,
                    duration
            );
            effects.add(effect);
        }
        // Para ataques à distância
        else if (broadcast.attackDef.isRanged()) {
            float dx = broadcast.targetX - broadcast.attackerX;
            float dy = broadcast.targetY - broadcast.attackerY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            float projectileSpeed = broadcast.attackDef.getProjectileSpeed();
            if (projectileSpeed <= 0) projectileSpeed = 600f;

            float flightDuration = distance / projectileSpeed;
            flightDuration = Math.min(0.5f, Math.max(0.1f, flightDuration));

            AttackEffect.EffectType projectileEffectType;
            String projectileId = broadcast.attackDef.getProjectileId();
            if (projectileId == null) projectileId = "arrow";

            switch (projectileId) {
                case "fireball":
                    projectileEffectType = AttackEffect.EffectType.FIREBALL;
                    break;
                case "arrow":
                    projectileEffectType = AttackEffect.EffectType.ARROW;
                    break;
                default:
                    projectileEffectType = AttackEffect.EffectType.PROJECTILE;
            }

            AttackEffect projectile = new AttackEffect(
                    projectileEffectType,
                    broadcast.attackerX,
                    broadcast.attackerY,
                    broadcast.targetX,
                    broadcast.targetY,
                    flightDuration
            );
            projectile.setProjectileType(projectileId);
            effects.add(projectile);

            // Efeito de disparo
            AttackEffect shootEffect = new AttackEffect(
                    AttackEffect.EffectType.BOW_SHOOT,
                    broadcast.attackerX,
                    broadcast.attackerY,
                    broadcast.targetX,
                    broadcast.targetY,
                    0.1f
            );
            effects.add(shootEffect);

            // Efeito de impacto
            if (broadcast.results != null && !broadcast.results.isEmpty()) {
                AttackResult result = broadcast.results.get(0);
                AttackEffect impactEffect = new AttackEffect(
                        AttackEffect.EffectType.DAGGER_STAB,
                        result.getTargetX() != 0 ? result.getTargetX() : broadcast.targetX,
                        result.getTargetY() != 0 ? result.getTargetY() : broadcast.targetY,
                        (result.getTargetX() != 0 ? result.getTargetX() : broadcast.targetX) + 10,
                        result.getTargetY() != 0 ? result.getTargetY() : broadcast.targetY,
                        0.1f
                );
                effects.add(impactEffect);
            }
        }
    }

    public void update(float delta) {
        effects.removeIf(effect -> {
            effect.update(delta);
            return !effect.active;
        });
    }

    // Renderiza elementos que usam LINE (bordas, linhas)
    public void renderLines(ShapeRenderer shapeRenderer) {
        for (AttackEffect effect : effects) {
            effect.renderLines(shapeRenderer);
        }
    }

    // Renderiza elementos que usam FILLED (círculos, retângulos preenchidos)
    public void renderFilled(ShapeRenderer shapeRenderer) {
        for (AttackEffect effect : effects) {
            effect.renderFilled(shapeRenderer);
        }
    }

    // Método legado para compatibilidade (não usar mais)
    @Deprecated
    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        // Não fazer nada - usar renderLines e renderFilled separadamente
    }

    public void clear() {
        effects.clear();
    }
}