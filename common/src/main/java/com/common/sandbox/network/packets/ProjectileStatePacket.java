package com.common.sandbox.network.packets;

import com.common.sandbox.model.Projectile;
import com.common.sandbox.network.Packet;

public class ProjectileStatePacket extends Packet {
    public String projectileId;
    public String ownerId;
    public String ownerName;
    public String projectileType;
    public String animationId;
    public float startX;
    public float startY;
    public float currentX;
    public float currentY;
    public float targetX;
    public float targetY;
    public float directionX;
    public float directionY;
    public float speed;
    public float maxDistance;
    public float distanceTraveled;
    public float damage;
    public boolean wasCritical;
    public long spawnTime;
    public boolean active;
    public int direction; // Direção fixa calculada no início

    public ProjectileStatePacket() {}

    public ProjectileStatePacket(Projectile projectile) {
        this.projectileId = projectile.getId();
        this.ownerId = projectile.getOwnerId();
        this.ownerName = projectile.getOwnerName();
        this.projectileType = projectile.getProjectileType();
        this.animationId = projectile.getAnimationId();
        this.startX = projectile.getStartX();
        this.startY = projectile.getStartY();
        this.currentX = projectile.getCurrentX();
        this.currentY = projectile.getCurrentY();
        this.targetX = projectile.getTargetX();
        this.targetY = projectile.getTargetY();
        this.directionX = projectile.getDirectionX();
        this.directionY = projectile.getDirectionY();
        this.speed = projectile.getSpeed();
        this.maxDistance = projectile.getMaxDistance();
        this.distanceTraveled = projectile.getDistanceTraveled();
        this.damage = projectile.getDamage();
        this.wasCritical = projectile.isWasCritical();
        this.spawnTime = projectile.getSpawnTime();
        this.active = projectile.isActive();
        this.direction = projectile.getDirection(); // Direção fixa
    }
}