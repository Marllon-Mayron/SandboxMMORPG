package com.common.sandbox.model.item;

import com.common.sandbox.model.enums.ArmorSet;
import com.common.sandbox.model.enums.Rarity;

import java.io.Serializable;

public class ItemDefinition implements Serializable {
    private static final long serialVersionUID = 2L;

    // ==================== DADOS BÁSICOS ====================
    private String id;
    private String name;
    private String description;
    private String category;     // "weapon", "consumable", "armor", "accessory", "equipment", "quest"
    private String rarityId = "common";
    private transient Rarity rarity;
    private String spritesheet;
    private int tileX;
    private int tileY;
    private int width = 32;
    private int height = 32;

    // ==================== SISTEMA DE SLOTS ====================
    private String armorSlot;        // "helmet", "chest", "legs", "boots"
    private String accessorySlot;    // "ring", "necklace", "cloak", "trinket"

    // ==================== SISTEMA DE CONJUNTOS ====================
    private String setId;
    private transient ArmorSet armorSet;

    // ==================== PROPRIEDADES DE COMBATE ====================
    private int damage;
    private int healAmount;
    private int staminaAmount;
    private int manaAmount;
    private int duration;

    // ==================== SISTEMA DE ATAQUES ====================
    private String attackId;
    private String attackAnimation;
    private float attackSpeed = 1.0f;
    private float attackCooldown = 1.0f;
    private boolean isRanged = false;
    private boolean isMagic = false;
    private int manaCost;
    private int staminaCost;

    // ==================== PARA ARMAS DE LONGO ALCANCE ====================
    private String projectileId;
    private float projectileSpeed = 600f;
    private float projectileRange = 400f;
    private String projectileAnimationId;
    private float hitboxDuration = 0.25f;

    // ==================== BÔNUS DE ATRIBUTOS ====================
    // Recursos
    private int bonusMaxHp = 0;
    private int bonusMaxMana = 0;
    private int bonusMaxStamina = 0;

    // Regeneração
    private int bonusHpRegen = 0;
    private int bonusManaRegen = 0;
    private int bonusStaminaRegen = 0;

    // Defesas
    private int bonusPhysicalDefense = 0;
    private int bonusMagicDefense = 0;

    // Poder de Dano
    private int bonusPhysicalPower = 0;
    private int bonusRangedPower = 0;
    private int bonusMagicPower = 0;

    // Chance e Multiplicadores
    private float bonusCriticalChance = 0f;
    private float bonusCriticalDamage = 0f;
    private float bonusDodgeChance = 0f;

    // Velocidades
    private float bonusAttackSpeed = 0f;
    private float bonusMovementSpeed = 0f;

    // Utilidades
    private float bonusCooldownReduction = 0f;
    private float bonusLifeSteal = 0f;
    private float bonusManaSteal = 0f;
    private float bonusTenacity = 0f;

    // Sorte
    private int bonusLuck = 0;

    // Resistências Elementais
    private int bonusFireResistance = 0;
    private int bonusIceResistance = 0;
    private int bonusLightningResistance = 0;
    private int bonusPoisonResistance = 0;
    private int bonusHolyResistance = 0;
    private int bonusDarkResistance = 0;

    // ==================== CONSTRUTOR ====================
    public ItemDefinition() {}

    // ==================== GETTERS E SETTERS ====================

    // Dados básicos
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getRarityId() { return rarityId; }

    public void setRarityId(String rarityId) {
        this.rarityId = rarityId;
        this.rarity = null;
    }

    public Rarity getRarity() {
        if (rarity == null && rarityId != null) {
            rarity = Rarity.fromId(rarityId);
        }
        return rarity != null ? rarity : Rarity.COMMON;
    }

    public void setRarity(Rarity rarity) {
        this.rarity = rarity;
        this.rarityId = rarity != null ? rarity.getId() : "common";
    }

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

    // Sistema de slots
    public String getArmorSlot() { return armorSlot; }
    public void setArmorSlot(String armorSlot) { this.armorSlot = armorSlot; }

    public String getAccessorySlot() { return accessorySlot; }
    public void setAccessorySlot(String accessorySlot) { this.accessorySlot = accessorySlot; }

    // Sistema de conjuntos
    public String getSetId() { return setId; }
    public void setSetId(String setId) {
        this.setId = setId;
        this.armorSet = null;
    }

    public ArmorSet getArmorSet() {
        if (armorSet == null && setId != null) {
            armorSet = ArmorSet.fromId(setId);
        }
        return armorSet;
    }

    public void setArmorSet(ArmorSet set) {
        this.armorSet = set;
        this.setId = set != null ? set.getId() : null;
    }

    // Propriedades de combate
    public int getDamage() { return damage; }
    public void setDamage(int damage) { this.damage = damage; }

    public int getHealAmount() { return healAmount; }
    public void setHealAmount(int healAmount) { this.healAmount = healAmount; }

    public int getManaAmount() { return manaAmount; }
    public void setManaAmount(int manaAmount) { this.manaAmount = manaAmount; }

    public int getStaminaAmount() { return staminaAmount; }
    public void setStaminaAmount(int staminaAmount) { this.staminaAmount = staminaAmount; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    // Sistema de ataques
    public String getAttackId() { return attackId; }
    public void setAttackId(String attackId) { this.attackId = attackId; }

    public String getAttackAnimation() { return attackAnimation; }
    public void setAttackAnimation(String attackAnimation) { this.attackAnimation = attackAnimation; }

    public float getAttackSpeed() { return attackSpeed; }
    public void setAttackSpeed(float attackSpeed) {
        this.attackSpeed = attackSpeed;
        if (attackSpeed > 0) {
            this.attackCooldown = 1.0f / attackSpeed;
        }
    }

    public float getAttackCooldown() { return attackCooldown; }
    public void setAttackCooldown(float attackCooldown) {
        this.attackCooldown = attackCooldown;
        if (attackCooldown > 0) {
            this.attackSpeed = 1.0f / attackCooldown;
        }
    }

    public boolean isRanged() { return isRanged; }
    public void setRanged(boolean ranged) { isRanged = ranged; }

    public boolean isMagic() { return isMagic; }
    public void setMagic(boolean magic) { isMagic = magic; }

    public int getManaCost() {
        return manaCost;
    }

    public void setManaCost(int manaCost) {
        this.manaCost = manaCost;
    }

    public int getStaminaCost() {
        return staminaCost;
    }

    public void setStaminaCost(int staminaCost) {
        this.staminaCost = staminaCost;
    }

    // Projéteis
    public String getProjectileId() { return projectileId; }
    public void setProjectileId(String projectileId) { this.projectileId = projectileId; }

    public float getProjectileSpeed() { return projectileSpeed; }
    public void setProjectileSpeed(float projectileSpeed) { this.projectileSpeed = projectileSpeed; }

    public float getProjectileRange() { return projectileRange; }
    public void setProjectileRange(float projectileRange) { this.projectileRange = projectileRange; }

    public String getProjectileAnimationId() { return projectileAnimationId; }
    public void setProjectileAnimationId(String projectileAnimationId) { this.projectileAnimationId = projectileAnimationId; }

    public float getHitboxDuration() { return hitboxDuration; }
    public void setHitboxDuration(float hitboxDuration) { this.hitboxDuration = hitboxDuration; }

    // ==================== BÔNUS DE ATRIBUTOS ====================

    // Recursos
    public int getBonusMaxHp() { return bonusMaxHp; }
    public void setBonusMaxHp(int bonusMaxHp) { this.bonusMaxHp = bonusMaxHp; }

    public int getBonusMaxMana() { return bonusMaxMana; }
    public void setBonusMaxMana(int bonusMaxMana) { this.bonusMaxMana = bonusMaxMana; }

    public int getBonusMaxStamina() { return bonusMaxStamina; }
    public void setBonusMaxStamina(int bonusMaxStamina) { this.bonusMaxStamina = bonusMaxStamina; }

    // Regeneração
    public int getBonusHpRegen() { return bonusHpRegen; }
    public void setBonusHpRegen(int bonusHpRegen) { this.bonusHpRegen = bonusHpRegen; }

    public int getBonusManaRegen() { return bonusManaRegen; }
    public void setBonusManaRegen(int bonusManaRegen) { this.bonusManaRegen = bonusManaRegen; }

    public int getBonusStaminaRegen() { return bonusStaminaRegen; }
    public void setBonusStaminaRegen(int bonusStaminaRegen) { this.bonusStaminaRegen = bonusStaminaRegen; }

    // Defesas
    public int getBonusPhysicalDefense() { return bonusPhysicalDefense; }
    public void setBonusPhysicalDefense(int bonusPhysicalDefense) { this.bonusPhysicalDefense = bonusPhysicalDefense; }

    public int getBonusMagicDefense() { return bonusMagicDefense; }
    public void setBonusMagicDefense(int bonusMagicDefense) { this.bonusMagicDefense = bonusMagicDefense; }

    // Poder de Dano
    public int getBonusPhysicalPower() { return bonusPhysicalPower; }
    public void setBonusPhysicalPower(int bonusPhysicalPower) { this.bonusPhysicalPower = bonusPhysicalPower; }

    public int getBonusRangedPower() { return bonusRangedPower; }
    public void setBonusRangedPower(int bonusRangedPower) { this.bonusRangedPower = bonusRangedPower; }

    public int getBonusMagicPower() { return bonusMagicPower; }
    public void setBonusMagicPower(int bonusMagicPower) { this.bonusMagicPower = bonusMagicPower; }

    // Chance e Multiplicadores
    public float getBonusCriticalChance() { return bonusCriticalChance; }
    public void setBonusCriticalChance(float bonusCriticalChance) { this.bonusCriticalChance = bonusCriticalChance; }

    public float getBonusCriticalDamage() { return bonusCriticalDamage; }
    public void setBonusCriticalDamage(float bonusCriticalDamage) { this.bonusCriticalDamage = bonusCriticalDamage; }

    public float getBonusDodgeChance() { return bonusDodgeChance; }
    public void setBonusDodgeChance(float bonusDodgeChance) { this.bonusDodgeChance = bonusDodgeChance; }

    // Velocidades
    public float getBonusAttackSpeed() { return bonusAttackSpeed; }
    public void setBonusAttackSpeed(float bonusAttackSpeed) { this.bonusAttackSpeed = bonusAttackSpeed; }

    public float getBonusMovementSpeed() { return bonusMovementSpeed; }
    public void setBonusMovementSpeed(float bonusMovementSpeed) { this.bonusMovementSpeed = bonusMovementSpeed; }

    // Utilidades
    public float getBonusCooldownReduction() { return bonusCooldownReduction; }
    public void setBonusCooldownReduction(float bonusCooldownReduction) { this.bonusCooldownReduction = bonusCooldownReduction; }

    public float getBonusLifeSteal() { return bonusLifeSteal; }
    public void setBonusLifeSteal(float bonusLifeSteal) { this.bonusLifeSteal = bonusLifeSteal; }

    public float getBonusManaSteal() { return bonusManaSteal; }
    public void setBonusManaSteal(float bonusManaSteal) { this.bonusManaSteal = bonusManaSteal; }

    public float getBonusTenacity() { return bonusTenacity; }
    public void setBonusTenacity(float bonusTenacity) { this.bonusTenacity = bonusTenacity; }

    // Sorte
    public int getBonusLuck() { return bonusLuck; }
    public void setBonusLuck(int bonusLuck) { this.bonusLuck = bonusLuck; }

    // Resistências Elementais
    public int getBonusFireResistance() { return bonusFireResistance; }
    public void setBonusFireResistance(int bonusFireResistance) { this.bonusFireResistance = bonusFireResistance; }

    public int getBonusIceResistance() { return bonusIceResistance; }
    public void setBonusIceResistance(int bonusIceResistance) { this.bonusIceResistance = bonusIceResistance; }

    public int getBonusLightningResistance() { return bonusLightningResistance; }
    public void setBonusLightningResistance(int bonusLightningResistance) { this.bonusLightningResistance = bonusLightningResistance; }

    public int getBonusPoisonResistance() { return bonusPoisonResistance; }
    public void setBonusPoisonResistance(int bonusPoisonResistance) { this.bonusPoisonResistance = bonusPoisonResistance; }

    public int getBonusHolyResistance() { return bonusHolyResistance; }
    public void setBonusHolyResistance(int bonusHolyResistance) { this.bonusHolyResistance = bonusHolyResistance; }

    public int getBonusDarkResistance() { return bonusDarkResistance; }
    public void setBonusDarkResistance(int bonusDarkResistance) { this.bonusDarkResistance = bonusDarkResistance; }

    // ==================== MÉTODOS ÚTEIS ====================

    public boolean isEquippable() {
        return "weapon".equals(category) || "armor".equals(category) || "accessory".equals(category) || "equipment".equals(category);
    }

    public boolean isConsumable() {
        return "consumable".equals(category);
    }

    public boolean isWeapon() {
        return "weapon".equals(category);
    }

    public boolean isArmor() {
        return "armor".equals(category);
    }

    public boolean isAccessory() {
        return "accessory".equals(category);
    }

    public boolean isMeleeWeapon() {
        return "weapon".equals(category) && !isRanged && !isMagic;
    }

    public boolean isRangedWeapon() {
        return "weapon".equals(category) && isRanged;
    }

    public boolean isMagicWeapon() {
        return "weapon".equals(category) && isMagic;
    }

    public boolean hasSet() {
        return setId != null && !setId.isEmpty();
    }

    public boolean isRing() {
        return "accessory".equals(category) && "ring".equals(accessorySlot);
    }

    public boolean isNecklace() {
        return "accessory".equals(category) && "necklace".equals(accessorySlot);
    }

    public boolean isCloak() {
        return "accessory".equals(category) && "cloak".equals(accessorySlot);
    }

    public boolean isTrinket() {
        return "accessory".equals(category) && "trinket".equals(accessorySlot);
    }

    @Override
    public String toString() {
        return String.format("ItemDefinition{id='%s', name='%s', category='%s', armorSlot='%s', accessorySlot='%s', damage=%d}",
                id, name, category, armorSlot, accessorySlot, damage);
    }
}