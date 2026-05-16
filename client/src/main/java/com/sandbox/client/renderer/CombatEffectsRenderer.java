package com.sandbox.client.renderer;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.common.sandbox.model.enums.AttackType;
import com.common.sandbox.network.packets.combat.AttackBroadcast;
import com.sandbox.client.camera.GameCamera;

import java.util.concurrent.ConcurrentLinkedQueue;

public class CombatEffectsRenderer {
    private static final float DAMAGE_NUMBER_DURATION = 1.0f;
    private static final float SWING_DURATION = 0.2f;

    private final ConcurrentLinkedQueue<DamageNumber> damageNumbers = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<SwingEffect> swingEffects = new ConcurrentLinkedQueue<>();

    public CombatEffectsRenderer() {}

    public void addDamageNumber(float x, float y, int damage, boolean critical) {
        damageNumbers.add(new DamageNumber(x, y, damage, critical));
    }

    public void addSwingEffect(float x, float y, String direction, AttackType type) {
        swingEffects.add(new SwingEffect(x, y, direction, type));
    }

    public void addAttackEffect(AttackBroadcast broadcast) {
        if (broadcast.getResult() != null && broadcast.getResult().isSuccess()) {
            addDamageNumber(broadcast.getAttackerX(), broadcast.getAttackerY(),
                    broadcast.getResult().getDamage(), broadcast.getResult().isWasCritical());
            addSwingEffect(broadcast.getAttackerX(), broadcast.getAttackerY(),
                    "RIGHT", broadcast.getResult().getAttackType());
        }
    }

    public void update(float delta) {
        damageNumbers.removeIf(dn -> {
            dn.life -= delta;
            return dn.life <= 0;
        });

        swingEffects.removeIf(swing -> {
            swing.life -= delta;
            return swing.life <= 0;
        });
    }

    /**
     * RENDER - Deve ser chamado DENTRO do batch.begin() / batch.end()
     */
    public void render(SpriteBatch batch, GameCamera camera, BitmapFont font) {
        if (batch == null || font == null) return;

        // Salvar a cor original
        Color originalColor = batch.getColor();

        for (DamageNumber dn : damageNumbers) {
            float alpha = dn.life / DAMAGE_NUMBER_DURATION;
            float yOffset = (1 - alpha) * 40;

            if (dn.critical) {
                font.setColor(1f, 0.2f, 0.2f, alpha);
            } else {
                font.setColor(1f, 0.8f, 0.2f, alpha);
            }

            font.draw(batch, String.valueOf(dn.damage), dn.x - 15, dn.y + yOffset);
        }

        for (SwingEffect swing : swingEffects) {
            float progress = 1.0f - (swing.life / SWING_DURATION);
            float alpha = 1f - progress * 0.5f;

            String slashChar = getSlashChar(progress);

            float offsetX = 0, offsetY = 0;
            switch (swing.direction) {
                case "RIGHT":
                    offsetX = 35 + progress * 15;
                    break;
                case "LEFT":
                    offsetX = -35 - progress * 15;
                    break;
                case "UP":
                    offsetY = 35 + progress * 15;
                    break;
                case "DOWN":
                    offsetY = -35 - progress * 15;
                    break;
                default:
                    offsetX = 35 + progress * 15;
            }

            font.setColor(1f, 0.6f + progress * 0.4f, 0.2f, alpha);
            font.draw(batch, slashChar, swing.x + offsetX, swing.y + offsetY);
        }

        // Restaurar cor original
        font.setColor(originalColor);
    }

    private String getSlashChar(float progress) {
        int frame = (int)(progress * 3);
        if (frame == 0) return "✧";
        if (frame == 1) return "✦";
        return "⚡";
    }

    public void dispose() {
        damageNumbers.clear();
        swingEffects.clear();
    }

    private static class DamageNumber {
        float x, y;
        int damage;
        boolean critical;
        float life = DAMAGE_NUMBER_DURATION;

        DamageNumber(float x, float y, int damage, boolean critical) {
            this.x = x;
            this.y = y;
            this.damage = damage;
            this.critical = critical;
        }
    }

    private static class SwingEffect {
        float x, y;
        String direction;
        AttackType type;
        float life = SWING_DURATION;

        SwingEffect(float x, float y, String direction, AttackType type) {
            this.x = x;
            this.y = y;
            this.direction = direction;
            this.type = type;
        }
    }
}