package com.sandbox.client.renderer.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;

public class AttackEffect {
    public enum EffectType {
        SWORD_SLASH,    // Traço de espada
        DAGGER_STAB,    // Ponto de adaga
        BOW_SHOOT,      // Linha de disparo
        PROJECTILE,     // Projétil viajando
        FIREBALL,       // Bola de fogo
        ARROW           // Flecha
    }

    public EffectType type;
    public Vector2 startPos;
    public Vector2 endPos;
    public Vector2 currentPos;
    public float progress;
    public float duration;
    public float timer;
    public boolean active;
    public String direction;
    public Color color;
    public String projectileType; // "arrow", "fireball", "bullet"

    public AttackEffect(EffectType type, float startX, float startY, float endX, float endY, float duration) {
        this.type = type;
        this.startPos = new Vector2(startX, startY);
        this.endPos = new Vector2(endX, endY);
        this.currentPos = new Vector2(startX, startY);
        this.progress = 0;
        this.duration = duration;
        this.timer = 0;
        this.active = true;
        this.color = new Color(1, 1, 1, 1);
        this.projectileType = "arrow";

        // Calcular direção
        float dx = endX - startX;
        float dy = endY - startY;
        if (Math.abs(dx) > Math.abs(dy)) {
            direction = dx > 0 ? "RIGHT" : "LEFT";
        } else {
            direction = dy > 0 ? "UP" : "DOWN";
        }
    }

    public void setProjectileType(String type) {
        this.projectileType = type;
        switch (type) {
            case "fireball":
                this.color.set(1f, 0.3f, 0.1f, 1f);
                break;
            case "arrow":
                this.color.set(0.8f, 0.6f, 0.2f, 1f);
                break;
            case "bullet":
                this.color.set(0.9f, 0.9f, 0.2f, 1f);
                break;
            default:
                this.color.set(1f, 1f, 1f, 1f);
        }
    }

    public void update(float delta) {
        if (!active) return;
        timer += delta;
        progress = Math.min(1.0f, timer / duration);

        // Para projéteis: interpolação linear do início ao fim
        if (type == EffectType.PROJECTILE || type == EffectType.ARROW || type == EffectType.FIREBALL) {
            currentPos.x = startPos.x + (endPos.x - startPos.x) * progress;
            currentPos.y = startPos.y + (endPos.y - startPos.y) * progress;
        }

        if (progress >= 1.0f) {
            active = false;
        }
    }

    public void render(SpriteBatch batch, ShapeRenderer shapeRenderer) {
        if (!active) return;

        float alpha = 1.0f - (progress * 0.5f);

        switch (type) {
            case SWORD_SLASH:
                renderSwordSlash(shapeRenderer, alpha);
                break;
            case DAGGER_STAB:
                renderDaggerStab(shapeRenderer, alpha);
                break;
            case BOW_SHOOT:
                renderBowShoot(shapeRenderer, alpha);
                break;
            case ARROW:
                renderArrow(shapeRenderer, alpha);
                break;
            case FIREBALL:
                renderFireball(shapeRenderer, alpha);
                break;
            case PROJECTILE:
                renderDefaultProjectile(shapeRenderer, alpha);
                break;
        }
    }

    private void renderSwordSlash(ShapeRenderer shapeRenderer, float alpha) {
        float centerX = (startPos.x + endPos.x) / 2;
        float centerY = (startPos.y + endPos.y) / 2;
        float angle = (float) Math.atan2(endPos.y - startPos.y, endPos.x - startPos.x);

        shapeRenderer.setColor(1f, 0.8f, 0.2f, alpha * 0.8f);
        for (int i = 0; i <= 10; i++) {
            float t = i / 10f;
            float x = startPos.x + (endPos.x - startPos.x) * t;
            float y = startPos.y + (endPos.y - startPos.y) * t;
            float offset = (float) Math.sin(t * Math.PI) * 15;
            float perpX = (float) Math.cos(angle + Math.PI/2) * offset;
            float perpY = (float) Math.sin(angle + Math.PI/2) * offset;
            shapeRenderer.circle(x + perpX, y + perpY, 4);
        }

        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        shapeRenderer.line(startPos.x, startPos.y, endPos.x, endPos.y);
    }

    private void renderDaggerStab(ShapeRenderer shapeRenderer, float alpha) {
        shapeRenderer.setColor(0.8f, 0.8f, 1f, alpha);
        shapeRenderer.circle(endPos.x, endPos.y, 8);
        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        shapeRenderer.circle(endPos.x, endPos.y, 4);
    }

    private void renderBowShoot(ShapeRenderer shapeRenderer, float alpha) {
        shapeRenderer.setColor(0.6f, 0.8f, 0.2f, alpha);
        shapeRenderer.line(startPos.x, startPos.y, endPos.x, endPos.y);
        shapeRenderer.setColor(1f, 1f, 0f, alpha);
        shapeRenderer.circle(startPos.x, startPos.y, 5);
    }

    private void renderArrow(ShapeRenderer shapeRenderer, float alpha) {
        // Flecha: um losango alongado na direção do movimento
        float angle = (float) Math.atan2(endPos.y - startPos.y, endPos.x - startPos.x);
        float arrowLength = 16;
        float arrowWidth = 6;

        // Ponto da flecha
        float tipX = currentPos.x + (float) Math.cos(angle) * 8;
        float tipY = currentPos.y + (float) Math.sin(angle) * 8;

        shapeRenderer.setColor(0.7f, 0.5f, 0.2f, alpha);

        // Corpo da flecha (linha)
        shapeRenderer.line(currentPos.x, currentPos.y, tipX, tipY);

        // Ponta da flecha (triângulo)
        float perpX = (float) Math.cos(angle + Math.PI/2) * 4;
        float perpY = (float) Math.sin(angle + Math.PI/2) * 4;

        shapeRenderer.setColor(0.9f, 0.7f, 0.3f, alpha);
        shapeRenderer.triangle(
                tipX, tipY,
                tipX - 8 * (float) Math.cos(angle) + perpX, tipY - 8 * (float) Math.sin(angle) + perpY,
                tipX - 8 * (float) Math.cos(angle) - perpX, tipY - 8 * (float) Math.sin(angle) - perpY
        );

        // Rastro (partículas)
        shapeRenderer.setColor(1f, 0.8f, 0.2f, alpha * 0.5f);
        shapeRenderer.circle(currentPos.x - 4 * (float) Math.cos(angle),
                currentPos.y - 4 * (float) Math.sin(angle), 3);
    }

    private void renderFireball(ShapeRenderer shapeRenderer, float alpha) {
        // Bola de fogo: círculo com chamas
        float size = 12;
        float angle = (float) Math.atan2(endPos.y - startPos.y, endPos.x - startPos.x);

        // Núcleo (branco/amarelo)
        shapeRenderer.setColor(1f, 0.9f, 0.2f, alpha);
        shapeRenderer.circle(currentPos.x, currentPos.y, size);

        // Chamas (laranja/vermelho)
        shapeRenderer.setColor(1f, 0.5f, 0.1f, alpha * 0.8f);
        shapeRenderer.circle(currentPos.x - 3, currentPos.y + 2, size - 4);
        shapeRenderer.circle(currentPos.x + 2, currentPos.y - 2, size - 5);

        // Rastro de fogo
        for (int i = 1; i <= 3; i++) {
            float trailX = currentPos.x - i * 8 * (float) Math.cos(angle);
            float trailY = currentPos.y - i * 8 * (float) Math.sin(angle);
            shapeRenderer.setColor(1f, 0.4f, 0.1f, alpha * (1 - i * 0.3f));
            shapeRenderer.circle(trailX, trailY, size - i * 3);
        }

        // Efeito de brilho
        shapeRenderer.setColor(1f, 1f, 0.5f, alpha * 0.3f);
        shapeRenderer.circle(currentPos.x, currentPos.y, size + 4);
    }

    private void renderDefaultProjectile(ShapeRenderer shapeRenderer, float alpha) {
        // Projétil padrão: quadrado colorido
        float size = 8;
        shapeRenderer.setColor(color.r, color.g, color.b, alpha);
        shapeRenderer.rect(currentPos.x - size/2, currentPos.y - size/2, size, size);

        // Brilho
        shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.5f);
        shapeRenderer.circle(currentPos.x, currentPos.y, size/2);
    }

    public void renderLines(ShapeRenderer shapeRenderer) {
        if (!active) return;

        float alpha = 1.0f - (progress * 0.5f);

        switch (type) {
            case SWORD_SLASH:
                renderSwordSlashLines(shapeRenderer, alpha);
                break;
            case BOW_SHOOT:
                renderBowShootLines(shapeRenderer, alpha);
                break;
            case ARROW:
                renderArrowLines(shapeRenderer, alpha);
                break;
            default:
                break;
        }
    }

    public void renderFilled(ShapeRenderer shapeRenderer) {
        if (!active) return;

        float alpha = 1.0f - (progress * 0.5f);

        switch (type) {
            case DAGGER_STAB:
                renderDaggerStabFilled(shapeRenderer, alpha);
                break;
            case ARROW:
                renderArrowFilled(shapeRenderer, alpha);
                break;
            case FIREBALL:
                renderFireballFilled(shapeRenderer, alpha);
                break;
            case PROJECTILE:
                renderDefaultProjectileFilled(shapeRenderer, alpha);
                break;
            default:
                break;
        }
    }

    private void renderSwordSlashLines(ShapeRenderer shapeRenderer, float alpha) {
        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        shapeRenderer.line(startPos.x, startPos.y, endPos.x, endPos.y);
    }

    private void renderBowShootLines(ShapeRenderer shapeRenderer, float alpha) {
        shapeRenderer.setColor(0.6f, 0.8f, 0.2f, alpha);
        shapeRenderer.line(startPos.x, startPos.y, endPos.x, endPos.y);
    }

    private void renderArrowLines(ShapeRenderer shapeRenderer, float alpha) {
        float angle = (float) Math.atan2(endPos.y - startPos.y, endPos.x - startPos.x);
        float tipX = currentPos.x + (float) Math.cos(angle) * 8;
        float tipY = currentPos.y + (float) Math.sin(angle) * 8;

        shapeRenderer.setColor(0.7f, 0.5f, 0.2f, alpha);
        shapeRenderer.line(currentPos.x, currentPos.y, tipX, tipY);
    }

    private void renderArrowFilled(ShapeRenderer shapeRenderer, float alpha) {
        float angle = (float) Math.atan2(endPos.y - startPos.y, endPos.x - startPos.x);
        float tipX = currentPos.x + (float) Math.cos(angle) * 8;
        float tipY = currentPos.y + (float) Math.sin(angle) * 8;
        float perpX = (float) Math.cos(angle + Math.PI/2) * 4;
        float perpY = (float) Math.sin(angle + Math.PI/2) * 4;

        shapeRenderer.setColor(0.9f, 0.7f, 0.3f, alpha);
        shapeRenderer.triangle(
                tipX, tipY,
                tipX - 8 * (float) Math.cos(angle) + perpX, tipY - 8 * (float) Math.sin(angle) + perpY,
                tipX - 8 * (float) Math.cos(angle) - perpX, tipY - 8 * (float) Math.sin(angle) - perpY
        );

        shapeRenderer.setColor(1f, 0.8f, 0.2f, alpha * 0.5f);
        shapeRenderer.circle(currentPos.x - 4 * (float) Math.cos(angle),
                currentPos.y - 4 * (float) Math.sin(angle), 3);
    }

    private void renderFireballFilled(ShapeRenderer shapeRenderer, float alpha) {
        float size = 12;

        shapeRenderer.setColor(1f, 0.9f, 0.2f, alpha);
        shapeRenderer.circle(currentPos.x, currentPos.y, size);

        shapeRenderer.setColor(1f, 0.5f, 0.1f, alpha * 0.8f);
        shapeRenderer.circle(currentPos.x - 3, currentPos.y + 2, size - 4);
        shapeRenderer.circle(currentPos.x + 2, currentPos.y - 2, size - 5);

        shapeRenderer.setColor(1f, 1f, 0.5f, alpha * 0.3f);
        shapeRenderer.circle(currentPos.x, currentPos.y, size + 4);
    }

    private void renderDefaultProjectileFilled(ShapeRenderer shapeRenderer, float alpha) {
        float size = 8;
        shapeRenderer.setColor(color.r, color.g, color.b, alpha);
        shapeRenderer.rect(currentPos.x - size/2, currentPos.y - size/2, size, size);
        shapeRenderer.setColor(1f, 1f, 1f, alpha * 0.5f);
        shapeRenderer.circle(currentPos.x, currentPos.y, size/2);
    }

    private void renderDaggerStabFilled(ShapeRenderer shapeRenderer, float alpha) {
        shapeRenderer.setColor(0.8f, 0.8f, 1f, alpha);
        shapeRenderer.circle(endPos.x, endPos.y, 8);
        shapeRenderer.setColor(1f, 1f, 1f, alpha);
        shapeRenderer.circle(endPos.x, endPos.y, 4);
    }
}