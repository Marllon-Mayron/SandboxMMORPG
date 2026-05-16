package com.common.sandbox.model.item;

import java.io.Serializable;

public class ItemDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String category;     // "weapon", "consumable", "armor", "quest"
    private String spritesheet;
    private int tileX;
    private int tileY;
    private int width = 32;
    private int height = 32;

    // Propriedades de combate
    private int damage;
    private int healAmount;
    private int duration;

    // Sistema de ataques
    private String attackId;           // ID do ataque que este item usa
    private String attackAnimation;    // Nome da animação
    private float attackSpeed = 1.0f;  // Velocidade de ataque (ataques por segundo)
    private float attackCooldown = 1.0f; //  Cooldown em segundos (calculado ou definido manualmente)
    private boolean isRanged = false;  // Se é arma de longo alcance

    // Para armas de longo alcance (projéteis)
    private String projectileId;       // "arrow", "fireball", "bullet", "ice_shard"
    private float projectileSpeed = 600f; // Velocidade do projétil (pixels/segundo)
    private float projectileRange = 400f; // Alcance máximo do projétil

    private String projectileAnimationId; // ID da animação do projétil ("arrow", "fireball", etc.)
    private float hitboxDuration = 0.25f; // Duração da hitbox em segundos
    // Estatísticas adicionais
    private int strengthBonus = 0;
    private int agilityBonus = 0;
    private int wisdomBonus = 0;

    public ItemDefinition() {}

    // ==================== GETTERS E SETTERS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getSpritesheet() { return spritesheet; }
    public void setSpritesheet(String spritesheet) { this.spritesheet = spritesheet; }

    public int getTileX() { return tileX; }
    public void setTileX(int tileX) { this.tileX = tileX; }

    public int getTileY() { return tileY; }
    public void setTileY(int tileY) { this.tileY = tileY; }

    public int getWidth() { return width; }
    public void setWidth(int width) { this.width = width; }

    public int getHeight() { return height; }
    public void setHeight(int height) { this.height = height; }

    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getAttackId() { return attackId; }
    public void setAttackId(String attackId) { this.attackId = attackId; }

    public String getAttackAnimation() { return attackAnimation; }
    public void setAttackAnimation(String attackAnimation) { this.attackAnimation = attackAnimation; }

    public float getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(float attackSpeed) {
        this.attackSpeed = attackSpeed;
        // Calcular cooldown automaticamente baseado no attackSpeed
        // Cooldown = 1 / AttackSpeed
        if (attackSpeed > 0) {
            this.attackCooldown = 1.0f / attackSpeed;
        }
    }

    public float getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
        // Se definir cooldown manualmente, recalcular attackSpeed
        if (attackCooldown > 0) {
            this.attackSpeed = 1.0f / attackCooldown;
        }
    }

    public boolean isRanged() { return isRanged; }
    public void setRanged(boolean ranged) { isRanged = ranged; }

    public String getProjectileId() { return projectileId; }
    public void setProjectileId(String projectileId) { this.projectileId = projectileId; }

    public float getProjectileSpeed() { return projectileSpeed; }
    public void setProjectileSpeed(float projectileSpeed) { this.projectileSpeed = projectileSpeed; }

    public float getProjectileRange() { return projectileRange; }
    public void setProjectileRange(float projectileRange) { this.projectileRange = projectileRange; }

    public int getStrengthBonus() { return strengthBonus; }
    public void setStrengthBonus(int strengthBonus) { this.strengthBonus = strengthBonus; }

    public int getAgilityBonus() { return agilityBonus; }
    public void setAgilityBonus(int agilityBonus) { this.agilityBonus = agilityBonus; }

    public int getWisdomBonus() { return wisdomBonus; }
    public void setWisdomBonus(int wisdomBonus) { this.wisdomBonus = wisdomBonus; }

    public String getProjectileAnimationId() { return projectileAnimationId; }
    public void setProjectileAnimationId(String projectileAnimationId) { this.projectileAnimationId = projectileAnimationId; }

    public float getHitboxDuration() { return hitboxDuration; }
    public void setHitboxDuration(float hitboxDuration) { this.hitboxDuration = hitboxDuration; }

}