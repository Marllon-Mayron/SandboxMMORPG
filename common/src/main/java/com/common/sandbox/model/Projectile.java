package com.common.sandbox.model;

import java.io.Serializable;
import java.util.UUID;

public class Projectile implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String ownerId;
    private String ownerName;
    private String projectileType;
    private String animationId;
    private float startX;
    private float startY;
    private float currentX;
    private float currentY;
    private float targetX;
    private float targetY;
    private float directionX;
    private float directionY;
    private float speed;
    private float maxDistance;
    private float distanceTraveled;
    private float damage;
    private boolean wasCritical;
    private long spawnTime;
    private boolean active;
    private int direction; // Direção calculada UMA VEZ

    public Projectile() {}

    public Projectile(String ownerId, String ownerName, String projectileType, String animationId,
                      float startX, float startY, float targetX, float targetY,
                      float speed, float maxDistance, float damage, boolean wasCritical) {
        this.id = UUID.randomUUID().toString();
        this.ownerId = ownerId;
        this.ownerName = ownerName;
        this.projectileType = projectileType;
        this.animationId = animationId;
        this.startX = startX;
        this.startY = startY;
        this.currentX = startX;
        this.currentY = startY;
        this.targetX = targetX;
        this.targetY = targetY;
        this.speed = speed;
        this.maxDistance = maxDistance;
        this.distanceTraveled = 0;
        this.damage = damage;
        this.wasCritical = wasCritical;
        this.spawnTime = System.currentTimeMillis();
        this.active = true;

        // Calcular direção UMA VEZ no construtor
        float dx = targetX - startX;
        float dy = targetY - startY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 0.01f) {
            this.directionX = dx / len;
            this.directionY = dy / len;
        } else {
            this.directionX = 0;
            this.directionY = 1;
        }

        // Calcular direção do sprite baseado no ângulo
        this.direction = calculateDirection(this.directionX, this.directionY);
    }

    private int calculateDirection(float dirX, float dirY) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len < 0.001f) return 0;

        float normX = dirX / len;
        float normY = dirY / len;

        // Sentido horário:
        // 0: Direita, 1: Direita-Baixo, 2: Baixo, 3: Esquerda-Baixo,
        // 4: Esquerda, 5: Esquerda-Cima, 6: Cima, 7: Direita-Cima

        // Para diagonais, comparar magnitude
        float absX = Math.abs(normX);
        float absY = Math.abs(normY);

        if (absX > absY) {
            // Horizontal dominante
            if (normX > 0) {
                // Direita
                if (normY > 0.3f) return 7;   // Direita-Baixo
                if (normY < -0.3f) return 1;  // Direita-Cima
                return 0;                      // Direita
            } else {
                // Esquerda
                if (normY > 0.3f) return 5;   // Esquerda-Baixo
                if (normY < -0.3f) return 3;  // Esquerda-Cima
                return 4;                      // Esquerda
            }
        } else {
            // Vertical dominante
            if (normY > 0) {
                // Baixo (Y positivo no jogo)
                if (normX > 0.3f) return 7;   // Direita-Baixo
                if (normX < -0.3f) return 5;  // Esquerda-Baixo
                return 2;                      // Baixo
            } else {
                // Cima (Y negativo no jogo)
                if (normX > 0.3f) return 1;   // Direita-Cima
                if (normX < -0.3f) return 3;  // Esquerda-Cima
                return 6;                      // Cima
            }
        }
    }

    public boolean update(float delta) {
        if (!active) return false;

        float moveDistance = speed * delta;
        float newX = currentX + directionX * moveDistance;
        float newY = currentY + directionY * moveDistance;

        float stepDistance = (float) Math.hypot(newX - currentX, newY - currentY);
        distanceTraveled += stepDistance;

        currentX = newX;
        currentY = newY;

        if (distanceTraveled >= maxDistance) {
            active = false;
            return false;
        }

        return true;
    }

    // Getters
    public String getId() { return id; }
    public String getOwnerId() { return ownerId; }
    public String getOwnerName() { return ownerName; }
    public String getProjectileType() { return projectileType; }
    public String getAnimationId() { return animationId; }
    public float getStartX() { return startX; }
    public float getStartY() { return startY; }
    public float getCurrentX() { return currentX; }
    public float getCurrentY() { return currentY; }
    public float getTargetX() { return targetX; }
    public float getTargetY() { return targetY; }
    public float getDirectionX() { return directionX; }
    public float getDirectionY() { return directionY; }
    public float getSpeed() { return speed; }
    public float getMaxDistance() { return maxDistance; }
    public float getDistanceTraveled() { return distanceTraveled; }
    public float getDamage() { return damage; }
    public boolean isWasCritical() { return wasCritical; }
    public long getSpawnTime() { return spawnTime; }
    public boolean isActive() { return active; }
    public int getDirection() { return direction; }

    // Setters
    public void setCurrentX(float currentX) { this.currentX = currentX; }
    public void setCurrentY(float currentY) { this.currentY = currentY; }
    public void setDistanceTraveled(float distanceTraveled) { this.distanceTraveled = distanceTraveled; }
    public void setActive(boolean active) { this.active = active; }
}