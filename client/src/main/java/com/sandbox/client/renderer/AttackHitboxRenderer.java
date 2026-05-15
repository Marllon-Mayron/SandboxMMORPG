package com.sandbox.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Circle;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.common.sandbox.model.AttackDefinition;
import com.common.sandbox.model.AttackHitboxType;
import com.sandbox.client.camera.GameCamera;

public class AttackHitboxRenderer {

    private boolean debugMode = true;

    // Hitbox atual - sempre inicializados
    private Rectangle currentRect;
    private Circle currentCircle;
    private float hitboxTimer;
    private AttackDefinition currentAttack;
    private Vector2 attackerPos;
    private Vector2 targetPos;
    private float directionAngle;

    public AttackHitboxRenderer() {
        this.currentRect = new Rectangle();
        this.currentCircle = new Circle();
        this.hitboxTimer = 0;
        this.attackerPos = new Vector2();
        this.targetPos = new Vector2();
    }

    /**
     * Mostra a hitbox do ataque baseado na definição
     */
    public void showHitbox(AttackDefinition attack, float attackerX, float attackerY,
                           float targetX, float targetY, float duration) {
        System.out.println("=== SHOWING HITBOX ===");
        System.out.println("Attack: " + attack.getName());
        System.out.println("Hitbox Type: " + attack.getHitboxType().getName());
        System.out.println("Attacker: (" + attackerX + ", " + attackerY + ")");
        System.out.println("Target: (" + targetX + ", " + targetY + ")");
        System.out.println("Duration: " + duration + "s");

        this.currentAttack = attack;
        this.attackerPos.set(attackerX, attackerY);
        this.targetPos.set(targetX, targetY);
        this.hitboxTimer = duration;

        // Calcular ângulo da direção (do jogador para o mouse)
        float dx = targetX - attackerX;
        float dy = targetY - attackerY;
        this.directionAngle = (float) Math.atan2(dy, dx);

        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        if (distance < 0.01f) {
            distance = 1;
        }

        // Criar hitbox baseada no tipo
        if (attack.getHitboxType() == AttackHitboxType.CIRCLE) {
            // Hitbox circular: centro na direção do mouse, limitado pelo range
            float range = attack.getRange();
            float actualDistance = Math.min(range, distance);
            float centerX = attackerX + (dx / distance) * actualDistance;
            float centerY = attackerY + (dy / distance) * actualDistance;

            if (currentCircle == null) {
                currentCircle = new Circle();
            }
            currentCircle.set(centerX, centerY, attack.getRadius());
            currentRect = null;

            System.out.println("Circle hitbox - Center: (" + centerX + ", " + centerY +
                    "), Radius: " + attack.getRadius());

        } else {
            // Hitbox retangular
            float halfWidth = attack.getWidth() / 2;
            float halfHeight = attack.getHeight() / 2;

            float centerDist = attack.getRange() / 2;
            float centerX = attackerX + (dx / distance) * centerDist;
            float centerY = attackerY + (dy / distance) * centerDist;

            if (currentRect == null) {
                currentRect = new Rectangle();
            }
            currentRect.set(centerX - halfWidth, centerY - halfHeight,
                    attack.getWidth(), attack.getHeight());
            currentCircle = null;

            System.out.println("Rectangle hitbox - Center: (" + centerX + ", " + centerY +
                    "), Size: " + attack.getWidth() + "x" + attack.getHeight());
        }
    }

    public void update(float delta) {
        if (hitboxTimer > 0) {
            hitboxTimer -= delta;
        }
    }

    /**
     * Renderiza a hitbox (chamar DENTRO do shapeRenderer.begin())
     */
    public void render(ShapeRenderer shapeRenderer) {
        if (!debugMode || hitboxTimer <= 0) return;

        float alpha = Math.min(1.0f, hitboxTimer / 0.5f); // Usar 0.5s como referência máxima

        Gdx.gl.glLineWidth(3f);

        if (currentCircle != null) {
            // Hitbox CIRCULAR
            shapeRenderer.setColor(1f, 0f, 0f, alpha * 0.3f);
            shapeRenderer.circle(currentCircle.x, currentCircle.y, currentCircle.radius);

            shapeRenderer.setColor(1f, 1f, 0f, alpha);
            shapeRenderer.circle(currentCircle.x, currentCircle.y, currentCircle.radius);

            shapeRenderer.setColor(1f, 0.5f, 0f, alpha);
            shapeRenderer.circle(currentCircle.x, currentCircle.y, currentCircle.radius - 5);
        }

        if (currentRect != null) {
            // Hitbox RETANGULAR
            shapeRenderer.setColor(1f, 0f, 0f, alpha * 0.3f);
            shapeRenderer.rect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);

            shapeRenderer.setColor(1f, 1f, 0f, alpha);
            shapeRenderer.rect(currentRect.x, currentRect.y, currentRect.width, currentRect.height);

            float centerX = currentRect.x + currentRect.width / 2;
            float centerY = currentRect.y + currentRect.height / 2;
            float lineEndX = centerX + (float) Math.cos(directionAngle) * currentRect.width / 1.5f;
            float lineEndY = centerY + (float) Math.sin(directionAngle) * currentRect.height / 1.5f;

            shapeRenderer.setColor(0f, 1f, 0f, alpha);
            shapeRenderer.line(centerX, centerY, lineEndX, lineEndY);
        }

        if (attackerPos != null && targetPos != null) {
            Gdx.gl.glLineWidth(2f);
            shapeRenderer.setColor(0f, 1f, 0f, alpha);
            shapeRenderer.line(attackerPos.x, attackerPos.y, targetPos.x, targetPos.y);

            shapeRenderer.setColor(0f, 1f, 0f, alpha);
            shapeRenderer.circle(attackerPos.x, attackerPos.y, 5);
            shapeRenderer.circle(targetPos.x, targetPos.y, 5);
        }

        Gdx.gl.glLineWidth(1f);
    }

    /**
     * Mostra texto informativo sobre o ataque
     */
    public void renderInfo(BitmapFont font, SpriteBatch batch, GameCamera camera) {
        if (!debugMode || hitboxTimer <= 0 || currentAttack == null || attackerPos == null) return;

        String info = String.format("⚔️ %s | %s | Range: %.0fpx",
                currentAttack.getName(),
                currentAttack.getHitboxType().getName(),
                currentAttack.getRange());

        String hitboxInfo = "";
        if (currentCircle != null) {
            hitboxInfo = String.format("🎯 Círculo | Raio: %.0fpx", currentCircle.radius);
        } else if (currentRect != null) {
            hitboxInfo = String.format("📐 Retângulo | %.0fx%.0fpx",
                    currentRect.width, currentRect.height);
        }

        font.setColor(Color.YELLOW);
        font.draw(batch, info, attackerPos.x - 100, attackerPos.y + 60);
        font.draw(batch, hitboxInfo, attackerPos.x - 100, attackerPos.y + 40);
        font.setColor(Color.WHITE);
    }

    public void setDebugMode(boolean enabled) {
        this.debugMode = enabled;
        System.out.println("🔍 Hitbox Debug: " + (enabled ? "ON" : "OFF"));
    }

    public boolean isDebugMode() {
        return debugMode;
    }

    public boolean isActive() {
        return hitboxTimer > 0;
    }
}