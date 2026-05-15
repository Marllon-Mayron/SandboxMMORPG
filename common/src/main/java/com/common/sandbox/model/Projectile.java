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
    private float angle;

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

        // Calcular direção normalizada (para movimento)
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

        // Calcular ângulo para rotação
        this.angle = calculateAngle(startX, startY, targetX, targetY);

        // Log para debug
        System.out.println("=== PROJECTILE ANGLE ===");
        System.out.println("Direction: (" + directionX + ", " + directionY + ")");
        System.out.println("Angle: " + this.angle);
        System.out.println("========================");
    }

    private float calculateAngle(float startX, float startY, float targetX, float targetY) {
        float dx = targetX - startX;
        float dy = targetY - startY;

        // Ângulo do movimento (0° = direita, 90° = cima no sistema matemático)
        float movementAngle = (float) Math.toDegrees(Math.atan2(dy, dx));

        float finalAngle = movementAngle + 45;

        // Normalizar para 0-360
        if (finalAngle < 0) finalAngle += 360;
        if (finalAngle >= 360) finalAngle -= 360;

        return finalAngle;
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
    public float getAngle() { return angle; }

    // Setters
    public void setCurrentX(float currentX) { this.currentX = currentX; }
    public void setCurrentY(float currentY) { this.currentY = currentY; }
    public void setDistanceTraveled(float distanceTraveled) { this.distanceTraveled = distanceTraveled; }
    public void setActive(boolean active) { this.active = active; }
}