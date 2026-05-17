package com.common.sandbox.model.player;

import com.common.sandbox.model.item.Inventory;
import com.common.sandbox.model.item.ItemDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

public class Player implements Serializable {
    private static final Logger logger = LoggerFactory.getLogger(Player.class);
    private static final long serialVersionUID = 5L;

    // ==================== DADOS BÁSICOS ====================
    private String id;
    private String username;
    private String email;
    private float x;
    private float y;
    private String direction;
    private boolean isOnline;

    // ==================== PROGRESSÃO ====================
    private int level;
    private int experience;
    private int gold;
    private int attributePoints;
    private int skillPoints;

    // ==================== INVENTÁRIO ====================
    private Inventory inventory;

    // ==================== VALORES BASE (Nível 1, sem pontos gastos) ====================
    private static final int BASE_MAX_HP = 100;
    private static final int BASE_MAX_MANA = 50;
    private static final int BASE_MAX_STAMINA = 100;

    private static final int BASE_HP_REGEN = 3;
    private static final int BASE_MANA_REGEN = 3;
    private static final int BASE_STAMINA_REGEN = 5;

    private static final int BASE_PHYSICAL_DEFENSE = 0;
    private static final int BASE_MAGIC_DEFENSE = 0;

    private static final int BASE_PHYSICAL_POWER = 10;
    private static final int BASE_RANGED_POWER = 10;
    private static final int BASE_MAGIC_POWER = 10;

    private static final float BASE_CRITICAL_CHANCE = 0.05f;
    private static final float BASE_CRITICAL_DAMAGE = 1.5f;
    private static final float BASE_DODGE_CHANCE = 0.0f;
    private static final float BASE_ATTACK_SPEED = 1.0f;
    private static final float BASE_MOVEMENT_SPEED = 400f;
    private static final float BASE_COOLDOWN_REDUCTION = 0.0f;
    private static final float BASE_LIFE_STEAL = 0.0f;
    private static final float BASE_MANA_STEAL = 0.0f;
    private static final float BASE_TENACITY = 0.0f;
    private static final int BASE_LUCK = 0;

    // ==================== BÔNUS DE ATRIBUTOS (GANHOS COM PONTOS DE ATRIBUTO) ====================
    private int bonusMaxHp;
    private int bonusMaxMana;
    private int bonusMaxStamina;

    private int bonusHpRegen;
    private int bonusManaRegen;
    private int bonusStaminaRegen;

    private int bonusPhysicalDefense;
    private int bonusMagicDefense;

    private int bonusPhysicalPower;
    private int bonusRangedPower;
    private int bonusMagicPower;

    private float bonusCriticalChance;
    private float bonusCriticalDamage;
    private float bonusDodgeChance;

    private float bonusAttackSpeed;
    private float bonusMovementSpeed;

    private float bonusCooldownReduction;
    private float bonusLifeSteal;
    private float bonusManaSteal;
    private float bonusTenacity;

    private int bonusLuck;

    // ==================== BÔNUS DE EQUIPAMENTOS (ARMA + ARMADURAS) ====================
    private int equipmentBonusMaxHp = 0;
    private int equipmentBonusMaxMana = 0;
    private int equipmentBonusMaxStamina = 0;

    private int equipmentBonusHpRegen = 0;
    private int equipmentBonusManaRegen = 0;
    private int equipmentBonusStaminaRegen = 0;

    private int equipmentBonusPhysicalDefense = 0;
    private int equipmentBonusMagicDefense = 0;

    private int equipmentBonusPhysicalPower = 0;
    private int equipmentBonusRangedPower = 0;
    private int equipmentBonusMagicPower = 0;

    private float equipmentBonusCriticalChance = 0f;
    private float equipmentBonusCriticalDamage = 0f;
    private float equipmentBonusDodgeChance = 0f;

    private float equipmentBonusAttackSpeed = 0f;
    private float equipmentBonusMovementSpeed = 0f;

    private float equipmentBonusCooldownReduction = 0f;
    private float equipmentBonusLifeSteal = 0f;
    private float equipmentBonusManaSteal = 0f;
    private float equipmentBonusTenacity = 0f;

    private int equipmentBonusLuck = 0;

    // ==================== RESISTÊNCIAS ELEMENTAIS (BÔNUS DE ATRIBUTO) ====================
    private int bonusFireResistance;
    private int bonusIceResistance;
    private int bonusLightningResistance;
    private int bonusPoisonResistance;
    private int bonusHolyResistance;
    private int bonusDarkResistance;

    // ==================== RESISTÊNCIAS ELEMENTAIS (BÔNUS DE EQUIPAMENTO) ====================
    private int equipmentBonusFireResistance = 0;
    private int equipmentBonusIceResistance = 0;
    private int equipmentBonusLightningResistance = 0;
    private int equipmentBonusPoisonResistance = 0;
    private int equipmentBonusHolyResistance = 0;
    private int equipmentBonusDarkResistance = 0;

    // ==================== STATUS ATUAIS ====================
    private int currentHp;
    private int currentMana;
    private int currentStamina;

    // ==================== DADOS TEMPORÁRIOS (NÃO PERSISTEM) ====================
    private transient long lastRegenTime;
    private transient long lastDashTime;
    private transient long lastMovementSend;
    private transient Integer lastChunkX;
    private transient Integer lastChunkY;
    private transient long lastSaveTime;
    private transient Runnable onStatusChanged;

    private transient CombatStats combatStats;
    private transient long lastAttackTime;
    private transient boolean isAttacking;
    private transient float attackTimer;
    private transient float currentAttackCooldown;

    private transient boolean isDashing;
    private transient float dashTimer;
    private transient float dashStartX;
    private transient float dashStartY;

    // ==================== CONSTANTES DO GAME ====================
    private static final float SPRINT_MULTIPLIER = 1.4f;
    private static final float SPRINT_STAMINA_COST = 0.001f;
    private static final int DASH_COOLDOWN_MS = 2000;
    private static final int DASH_DISTANCE = 150;
    private static final int DASH_STAMINA_COST = 20;
    private static final float DASH_DURATION = 0.15f;

    private static final int HP_PER_LEVEL = 10;
    private static final int MANA_PER_LEVEL = 5;
    private static final int STAMINA_PER_LEVEL = 5;

    // ==================== CONSTRUTOR ====================
    public Player() {
        this.id = "";
        this.username = "";
        this.email = "";
        this.x = 400;
        this.y = 300;
        this.direction = "DOWN";
        this.isOnline = true;

        this.level = 1;
        this.experience = 0;
        this.gold = 0;
        this.attributePoints = 0;
        this.skillPoints = 0;

        this.inventory = new Inventory();
        this.combatStats = new CombatStats();

        // Inicializar todos os bônus de atributo com zero
        this.bonusMaxHp = 0;
        this.bonusMaxMana = 0;
        this.bonusMaxStamina = 0;
        this.bonusHpRegen = 0;
        this.bonusManaRegen = 0;
        this.bonusStaminaRegen = 0;
        this.bonusPhysicalDefense = 0;
        this.bonusMagicDefense = 0;
        this.bonusPhysicalPower = 0;
        this.bonusRangedPower = 0;
        this.bonusMagicPower = 0;
        this.bonusCriticalChance = 0.0f;
        this.bonusCriticalDamage = 0.0f;
        this.bonusDodgeChance = 0.0f;
        this.bonusAttackSpeed = 0.0f;
        this.bonusMovementSpeed = 0.0f;
        this.bonusCooldownReduction = 0.0f;
        this.bonusLifeSteal = 0.0f;
        this.bonusManaSteal = 0.0f;
        this.bonusTenacity = 0.0f;
        this.bonusLuck = 0;

        this.bonusFireResistance = 0;
        this.bonusIceResistance = 0;
        this.bonusLightningResistance = 0;
        this.bonusPoisonResistance = 0;
        this.bonusHolyResistance = 0;
        this.bonusDarkResistance = 0;

        // Calcular valores totais
        this.currentHp = getMaxHp();
        this.currentMana = getMaxMana();
        this.currentStamina = getMaxStamina();

        // Timers
        long now = System.currentTimeMillis();
        this.lastRegenTime = now;
        this.lastDashTime = 0;
        this.lastMovementSend = 0;
        this.lastSaveTime = now;
        this.lastAttackTime = 0;
        this.isAttacking = false;
        this.attackTimer = 0;
        this.currentAttackCooldown = 1.0f;
    }

    // ==================== MÉTODOS DE CÁLCULO (TOTAL = BASE + BONUS_ATRIBUTO + BONUS_EQUIPAMENTO + LEVEL) ====================

    public int getMaxHp() {
        return BASE_MAX_HP + bonusMaxHp + equipmentBonusMaxHp + ((level - 1) * HP_PER_LEVEL);
    }

    public int getMaxMana() {
        return BASE_MAX_MANA + bonusMaxMana + equipmentBonusMaxMana + ((level - 1) * MANA_PER_LEVEL);
    }

    public int getMaxStamina() {
        return BASE_MAX_STAMINA + bonusMaxStamina + equipmentBonusMaxStamina + ((level - 1) * STAMINA_PER_LEVEL);
    }

    public int getHpRegen() {
        return BASE_HP_REGEN + bonusHpRegen + equipmentBonusHpRegen;
    }

    public int getManaRegen() {
        return BASE_MANA_REGEN + bonusManaRegen + equipmentBonusManaRegen;
    }

    public int getStaminaRegen() {
        return BASE_STAMINA_REGEN + bonusStaminaRegen + equipmentBonusStaminaRegen;
    }

    public int getPhysicalDefense() {
        return BASE_PHYSICAL_DEFENSE + bonusPhysicalDefense + equipmentBonusPhysicalDefense;
    }

    public int getMagicDefense() {
        return BASE_MAGIC_DEFENSE + bonusMagicDefense + equipmentBonusMagicDefense;
    }

    public int getPhysicalPower() {
        return BASE_PHYSICAL_POWER + bonusPhysicalPower + equipmentBonusPhysicalPower;
    }

    public int getRangedPower() {
        return BASE_RANGED_POWER + bonusRangedPower + equipmentBonusRangedPower;
    }

    public int getMagicPower() {
        return BASE_MAGIC_POWER + bonusMagicPower + equipmentBonusMagicPower;
    }

    public float getCriticalChance() {
        float total = BASE_CRITICAL_CHANCE + bonusCriticalChance + equipmentBonusCriticalChance;
        return Math.min(0.75f, total);
    }

    public float getCriticalDamage() {
        return BASE_CRITICAL_DAMAGE + bonusCriticalDamage + equipmentBonusCriticalDamage;
    }

    public float getDodgeChance() {
        float total = BASE_DODGE_CHANCE + bonusDodgeChance + equipmentBonusDodgeChance;
        return Math.min(0.50f, total);
    }

    public float getAttackSpeed() {
        return BASE_ATTACK_SPEED + bonusAttackSpeed + equipmentBonusAttackSpeed;
    }

    public float getMovementSpeed() {
        return BASE_MOVEMENT_SPEED + bonusMovementSpeed + equipmentBonusMovementSpeed;
    }

    public float getCooldownReduction() {
        float total = BASE_COOLDOWN_REDUCTION + bonusCooldownReduction + equipmentBonusCooldownReduction;
        return Math.min(0.50f, total);
    }

    public float getLifeSteal() {
        return BASE_LIFE_STEAL + bonusLifeSteal + equipmentBonusLifeSteal;
    }

    public float getManaSteal() {
        return BASE_MANA_STEAL + bonusManaSteal + equipmentBonusManaSteal;
    }

    public float getTenacity() {
        float total = BASE_TENACITY + bonusTenacity + equipmentBonusTenacity;
        return Math.min(0.60f, total);
    }

    public int getLuck() {
        return BASE_LUCK + bonusLuck + equipmentBonusLuck;
    }

    // Resistências elementais (total = bônus atributo + bônus equipamento)
    public int getFireResistance() { return bonusFireResistance + equipmentBonusFireResistance; }
    public int getIceResistance() { return bonusIceResistance + equipmentBonusIceResistance; }
    public int getLightningResistance() { return bonusLightningResistance + equipmentBonusLightningResistance; }
    public int getPoisonResistance() { return bonusPoisonResistance + equipmentBonusPoisonResistance; }
    public int getHolyResistance() { return bonusHolyResistance + equipmentBonusHolyResistance; }
    public int getDarkResistance() { return bonusDarkResistance + equipmentBonusDarkResistance; }

    // ==================== MÉTODOS PARA RESETAR BÔNUS DE EQUIPAMENTO ====================

    public void resetEquipmentBonuses() {
        equipmentBonusMaxHp = 0;
        equipmentBonusMaxMana = 0;
        equipmentBonusMaxStamina = 0;
        equipmentBonusHpRegen = 0;
        equipmentBonusManaRegen = 0;
        equipmentBonusStaminaRegen = 0;
        equipmentBonusPhysicalDefense = 0;
        equipmentBonusMagicDefense = 0;
        equipmentBonusPhysicalPower = 0;
        equipmentBonusRangedPower = 0;
        equipmentBonusMagicPower = 0;
        equipmentBonusCriticalChance = 0f;
        equipmentBonusCriticalDamage = 0f;
        equipmentBonusDodgeChance = 0f;
        equipmentBonusAttackSpeed = 0f;
        equipmentBonusMovementSpeed = 0f;
        equipmentBonusCooldownReduction = 0f;
        equipmentBonusLifeSteal = 0f;
        equipmentBonusManaSteal = 0f;
        equipmentBonusTenacity = 0f;
        equipmentBonusLuck = 0;
        equipmentBonusFireResistance = 0;
        equipmentBonusIceResistance = 0;
        equipmentBonusLightningResistance = 0;
        equipmentBonusPoisonResistance = 0;
        equipmentBonusHolyResistance = 0;
        equipmentBonusDarkResistance = 0;
    }

    // ==================== MÉTODOS DE BÔNUS DE EQUIPAMENTO (SETTERS) ====================

    public void addEquipmentBonus(ItemDefinition def) {
        if (def == null) return;

        equipmentBonusMaxHp += def.getBonusMaxHp();
        equipmentBonusMaxMana += def.getBonusMaxMana();
        equipmentBonusMaxStamina += def.getBonusMaxStamina();
        equipmentBonusHpRegen += def.getBonusHpRegen();
        equipmentBonusManaRegen += def.getBonusManaRegen();
        equipmentBonusStaminaRegen += def.getBonusStaminaRegen();
        equipmentBonusPhysicalDefense += def.getBonusPhysicalDefense();
        equipmentBonusMagicDefense += def.getBonusMagicDefense();
        equipmentBonusPhysicalPower += def.getBonusPhysicalPower();
        equipmentBonusRangedPower += def.getBonusRangedPower();
        equipmentBonusMagicPower += def.getBonusMagicPower();
        equipmentBonusCriticalChance += def.getBonusCriticalChance();
        equipmentBonusCriticalDamage += def.getBonusCriticalDamage();
        equipmentBonusDodgeChance += def.getBonusDodgeChance();
        equipmentBonusAttackSpeed += def.getBonusAttackSpeed();
        equipmentBonusMovementSpeed += def.getBonusMovementSpeed();
        equipmentBonusCooldownReduction += def.getBonusCooldownReduction();
        equipmentBonusLifeSteal += def.getBonusLifeSteal();
        equipmentBonusManaSteal += def.getBonusManaSteal();
        equipmentBonusTenacity += def.getBonusTenacity();
        equipmentBonusLuck += def.getBonusLuck();
        equipmentBonusFireResistance += def.getBonusFireResistance();
        equipmentBonusIceResistance += def.getBonusIceResistance();
        equipmentBonusLightningResistance += def.getBonusLightningResistance();
        equipmentBonusPoisonResistance += def.getBonusPoisonResistance();
        equipmentBonusHolyResistance += def.getBonusHolyResistance();
        equipmentBonusDarkResistance += def.getBonusDarkResistance();
    }

    public void removeEquipmentBonus(ItemDefinition def) {
        if (def == null) return;

        equipmentBonusMaxHp -= def.getBonusMaxHp();
        equipmentBonusMaxMana -= def.getBonusMaxMana();
        equipmentBonusMaxStamina -= def.getBonusMaxStamina();
        equipmentBonusHpRegen -= def.getBonusHpRegen();
        equipmentBonusManaRegen -= def.getBonusManaRegen();
        equipmentBonusStaminaRegen -= def.getBonusStaminaRegen();
        equipmentBonusPhysicalDefense -= def.getBonusPhysicalDefense();
        equipmentBonusMagicDefense -= def.getBonusMagicDefense();
        equipmentBonusPhysicalPower -= def.getBonusPhysicalPower();
        equipmentBonusRangedPower -= def.getBonusRangedPower();
        equipmentBonusMagicPower -= def.getBonusMagicPower();
        equipmentBonusCriticalChance -= def.getBonusCriticalChance();
        equipmentBonusCriticalDamage -= def.getBonusCriticalDamage();
        equipmentBonusDodgeChance -= def.getBonusDodgeChance();
        equipmentBonusAttackSpeed -= def.getBonusAttackSpeed();
        equipmentBonusMovementSpeed -= def.getBonusMovementSpeed();
        equipmentBonusCooldownReduction -= def.getBonusCooldownReduction();
        equipmentBonusLifeSteal -= def.getBonusLifeSteal();
        equipmentBonusManaSteal -= def.getBonusManaSteal();
        equipmentBonusTenacity -= def.getBonusTenacity();
        equipmentBonusLuck -= def.getBonusLuck();
        equipmentBonusFireResistance -= def.getBonusFireResistance();
        equipmentBonusIceResistance -= def.getBonusIceResistance();
        equipmentBonusLightningResistance -= def.getBonusLightningResistance();
        equipmentBonusPoisonResistance -= def.getBonusPoisonResistance();
        equipmentBonusHolyResistance -= def.getBonusHolyResistance();
        equipmentBonusDarkResistance -= def.getBonusDarkResistance();
    }

    // ==================== GETTERS E SETTERS DOS BÔNUS DE EQUIPAMENTO ====================

    public int getEquipmentBonusMaxHp() { return equipmentBonusMaxHp; }
    public void setEquipmentBonusMaxHp(int value) { this.equipmentBonusMaxHp = value; }

    public int getEquipmentBonusMaxMana() { return equipmentBonusMaxMana; }
    public void setEquipmentBonusMaxMana(int value) { this.equipmentBonusMaxMana = value; }

    public int getEquipmentBonusMaxStamina() { return equipmentBonusMaxStamina; }
    public void setEquipmentBonusMaxStamina(int value) { this.equipmentBonusMaxStamina = value; }

    public int getEquipmentBonusHpRegen() { return equipmentBonusHpRegen; }
    public void setEquipmentBonusHpRegen(int value) { this.equipmentBonusHpRegen = value; }

    public int getEquipmentBonusManaRegen() { return equipmentBonusManaRegen; }
    public void setEquipmentBonusManaRegen(int value) { this.equipmentBonusManaRegen = value; }

    public int getEquipmentBonusStaminaRegen() { return equipmentBonusStaminaRegen; }
    public void setEquipmentBonusStaminaRegen(int value) { this.equipmentBonusStaminaRegen = value; }

    public int getEquipmentBonusPhysicalDefense() { return equipmentBonusPhysicalDefense; }
    public void setEquipmentBonusPhysicalDefense(int value) { this.equipmentBonusPhysicalDefense = value; }

    public int getEquipmentBonusMagicDefense() { return equipmentBonusMagicDefense; }
    public void setEquipmentBonusMagicDefense(int value) { this.equipmentBonusMagicDefense = value; }

    public int getEquipmentBonusPhysicalPower() { return equipmentBonusPhysicalPower; }
    public void setEquipmentBonusPhysicalPower(int value) { this.equipmentBonusPhysicalPower = value; }

    public int getEquipmentBonusRangedPower() { return equipmentBonusRangedPower; }
    public void setEquipmentBonusRangedPower(int value) { this.equipmentBonusRangedPower = value; }

    public int getEquipmentBonusMagicPower() { return equipmentBonusMagicPower; }
    public void setEquipmentBonusMagicPower(int value) { this.equipmentBonusMagicPower = value; }

    public float getEquipmentBonusCriticalChance() { return equipmentBonusCriticalChance; }
    public void setEquipmentBonusCriticalChance(float value) { this.equipmentBonusCriticalChance = value; }

    public float getEquipmentBonusCriticalDamage() { return equipmentBonusCriticalDamage; }
    public void setEquipmentBonusCriticalDamage(float value) { this.equipmentBonusCriticalDamage = value; }

    public float getEquipmentBonusDodgeChance() { return equipmentBonusDodgeChance; }
    public void setEquipmentBonusDodgeChance(float value) { this.equipmentBonusDodgeChance = value; }

    public float getEquipmentBonusAttackSpeed() { return equipmentBonusAttackSpeed; }
    public void setEquipmentBonusAttackSpeed(float value) { this.equipmentBonusAttackSpeed = value; }

    public float getEquipmentBonusMovementSpeed() { return equipmentBonusMovementSpeed; }
    public void setEquipmentBonusMovementSpeed(float value) { this.equipmentBonusMovementSpeed = value; }

    public float getEquipmentBonusCooldownReduction() { return equipmentBonusCooldownReduction; }
    public void setEquipmentBonusCooldownReduction(float value) { this.equipmentBonusCooldownReduction = value; }

    public float getEquipmentBonusLifeSteal() { return equipmentBonusLifeSteal; }
    public void setEquipmentBonusLifeSteal(float value) { this.equipmentBonusLifeSteal = value; }

    public float getEquipmentBonusManaSteal() { return equipmentBonusManaSteal; }
    public void setEquipmentBonusManaSteal(float value) { this.equipmentBonusManaSteal = value; }

    public float getEquipmentBonusTenacity() { return equipmentBonusTenacity; }
    public void setEquipmentBonusTenacity(float value) { this.equipmentBonusTenacity = value; }

    public int getEquipmentBonusLuck() { return equipmentBonusLuck; }
    public void setEquipmentBonusLuck(int value) { this.equipmentBonusLuck = value; }

    public int getEquipmentBonusFireResistance() { return equipmentBonusFireResistance; }
    public void setEquipmentBonusFireResistance(int value) { this.equipmentBonusFireResistance = value; }

    public int getEquipmentBonusIceResistance() { return equipmentBonusIceResistance; }
    public void setEquipmentBonusIceResistance(int value) { this.equipmentBonusIceResistance = value; }

    public int getEquipmentBonusLightningResistance() { return equipmentBonusLightningResistance; }
    public void setEquipmentBonusLightningResistance(int value) { this.equipmentBonusLightningResistance = value; }

    public int getEquipmentBonusPoisonResistance() { return equipmentBonusPoisonResistance; }
    public void setEquipmentBonusPoisonResistance(int value) { this.equipmentBonusPoisonResistance = value; }

    public int getEquipmentBonusHolyResistance() { return equipmentBonusHolyResistance; }
    public void setEquipmentBonusHolyResistance(int value) { this.equipmentBonusHolyResistance = value; }

    public int getEquipmentBonusDarkResistance() { return equipmentBonusDarkResistance; }
    public void setEquipmentBonusDarkResistance(int value) { this.equipmentBonusDarkResistance = value; }

    // ==================== SISTEMA DE UPAR ATRIBUTOS ====================

    public static float getAttributeIncrement(String attributeName) {
        switch (attributeName.toLowerCase()) {
            case "max_hp": return 10f;
            case "max_mana": return 10f;
            case "max_stamina": return 10f;
            case "hp_regen": return 1f;
            case "mana_regen": return 1f;
            case "stamina_regen": return 1f;
            case "physical_defense": return 5f;
            case "magic_defense": return 5f;
            case "physical_power": return 3f;
            case "ranged_power": return 3f;
            case "magic_power": return 3f;
            case "critical_chance": return 0.005f;
            case "critical_damage": return 0.01f;
            case "dodge_chance": return 0.01f;
            case "attack_speed": return 0.02f;
            case "movement_speed": return 10f;
            case "cooldown_reduction": return 0.01f;
            case "life_steal": return 0.01f;
            case "mana_steal": return 0.01f;
            case "tenacity": return 0.01f;
            case "luck": return 5f;
            case "fire_resistance":
            case "ice_resistance":
            case "lightning_resistance":
            case "poison_resistance":
            case "holy_resistance":
            case "dark_resistance":
                return 5f;
            default: return 0f;
        }
    }

    public static String getFormattedAttributeIncrement(String attributeName) {
        float increment = getAttributeIncrement(attributeName);
        switch (attributeName.toLowerCase()) {
            case "critical_chance":
                if (increment < 1 && increment * 100 != (int)(increment * 100)) {
                    return "+" + (increment * 100) + "%";
                }
                return "+" + (int)(increment * 100) + "%";
            case "critical_damage":
            case "dodge_chance":
            case "cooldown_reduction":
            case "life_steal":
            case "mana_steal":
            case "tenacity":
            case "attack_speed":
                return "+" + (int)(increment * 100) + "%";
            case "movement_speed":
                return "+" + (int)increment + " px/s";
            default:
                if (increment < 1 && increment * 100 != (int)(increment * 100)) {
                    return "+" + (increment * 100) + "%";
                }
                return "+" + (int)increment;
        }
    }

    public static String[] getAvailableAttributes() {
        return new String[]{
                "max_hp", "max_mana", "max_stamina",
                "hp_regen", "mana_regen", "stamina_regen",
                "physical_defense", "magic_defense",
                "physical_power", "ranged_power", "magic_power",
                "critical_chance", "critical_damage", "dodge_chance",
                "attack_speed", "movement_speed",
                "cooldown_reduction", "life_steal", "mana_steal", "tenacity",
                "luck",
                "fire_resistance", "ice_resistance", "lightning_resistance",
                "poison_resistance", "holy_resistance", "dark_resistance"
        };
    }

    // ==================== REGENERAÇÃO ====================

    public void updateRegeneration(float delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRegenTime;

        if (elapsed >= 1000) {
            int seconds = (int) (elapsed / 1000);
            boolean changed = false;

            if (currentHp < getMaxHp()) {
                int regen = getHpRegen() * seconds;
                currentHp = Math.min(getMaxHp(), currentHp + regen);
                changed = true;
            }

            if (currentMana < getMaxMana()) {
                int regen = getManaRegen() * seconds;
                currentMana = Math.min(getMaxMana(), currentMana + regen);
                changed = true;
            }

            if (currentStamina < getMaxStamina()) {
                int regen = getStaminaRegen() * seconds;
                currentStamina = Math.min(getMaxStamina(), currentStamina + regen);
                changed = true;
            }

            if (changed) {
                notifyStatusChanged();
            }

            lastRegenTime = now - (elapsed % 1000);
        }
    }

    public void validateCurrentStats() {
        int maxHp = getMaxHp();
        int maxMana = getMaxMana();
        int maxStamina = getMaxStamina();

        if (currentHp > maxHp) currentHp = maxHp;
        if (currentMana > maxMana) currentMana = maxMana;
        if (currentStamina > maxStamina) currentStamina = maxStamina;
        if (currentHp < 0) currentHp = 0;
        if (currentMana < 0) currentMana = 0;
        if (currentStamina < 0) currentStamina = 0;
    }

    // ==================== EXPERIÊNCIA ====================

    public boolean addExperience(int amount) {
        ExperienceSystem.AddXpResult result = ExperienceSystem.addExperience(this, amount);
        if (result.hasLeveledUp()) {
            int levelsGained = result.levelsGained;
            logger.info("Player {} gained {} levels!", getUsername(), levelsGained);
            validateCurrentStats();
            notifyStatusChanged();
            return true;
        }
        return false;
    }

    public int getXpForNextLevel() {
        return ExperienceSystem.getRequiredXP(level + 1);
    }

    public float getXpProgress() {
        return ExperienceSystem.getProgressToNextLevel(experience, level);
    }

    // ==================== MOVIMENTO E DASH ====================

    public boolean canDash() {
        long now = System.currentTimeMillis();
        return (now - lastDashTime) >= DASH_COOLDOWN_MS && currentStamina >= DASH_STAMINA_COST;
    }

    public boolean executeDash() {
        if (!canDash()) return false;
        lastDashTime = System.currentTimeMillis();
        currentStamina -= DASH_STAMINA_COST;
        notifyStatusChanged();
        return true;
    }

    public boolean consumeStaminaForSprint(float delta) {
        float cost = SPRINT_STAMINA_COST * delta;
        if (currentStamina >= cost) {
            currentStamina -= cost;
            notifyStatusChanged();
            return true;
        }
        return false;
    }

    // ==================== ATAQUE ====================

    public boolean canAttack() {
        long now = System.currentTimeMillis();
        float cooldownSeconds = currentAttackCooldown * (1 - getCooldownReduction());
        long cooldownMillis = (long)(cooldownSeconds * 1000);
        return (now - lastAttackTime) >= cooldownMillis;
    }

    public boolean executeAttack() {
        if (!canAttack()) return false;
        lastAttackTime = System.currentTimeMillis();
        isAttacking = true;
        attackTimer = 0.3f;
        notifyStatusChanged();
        return true;
    }

    public void updateAttack(float delta) {
        if (isAttacking) {
            attackTimer -= delta;
            if (attackTimer <= 0) {
                isAttacking = false;
            }
        }
    }

    // ==================== GETTERS E SETTERS BÁSICOS ====================

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        validateCurrentStats();
        notifyStatusChanged();
    }

    public int getExperience() { return experience; }
    public void setExperience(int experience) {
        this.experience = experience;
        notifyStatusChanged();
    }

    public int getGold() { return gold; }
    public void setGold(int gold) {
        this.gold = gold;
        notifyStatusChanged();
    }

    public int getAttributePoints() { return attributePoints; }
    public void setAttributePoints(int attributePoints) {
        this.attributePoints = attributePoints;
        notifyStatusChanged();
    }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) {
        this.currentHp = Math.min(getMaxHp(), Math.max(0, currentHp));
        notifyStatusChanged();
    }

    public int getCurrentMana() { return currentMana; }
    public void setCurrentMana(int currentMana) {
        this.currentMana = Math.min(getMaxMana(), Math.max(0, currentMana));
        notifyStatusChanged();
    }

    public int getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(int currentStamina) {
        this.currentStamina = Math.min(getMaxStamina(), Math.max(0, currentStamina));
        notifyStatusChanged();
    }

    // ==================== GETTERS E SETTERS DOS BÔNUS DE ATRIBUTO ====================

    public int getBonusMaxHp() { return bonusMaxHp; }
    public void setBonusMaxHp(int bonusMaxHp) {
        this.bonusMaxHp = bonusMaxHp;
        validateCurrentStats();
        notifyStatusChanged();
    }

    public int getBonusMaxMana() { return bonusMaxMana; }
    public void setBonusMaxMana(int bonusMaxMana) {
        this.bonusMaxMana = bonusMaxMana;
        validateCurrentStats();
        notifyStatusChanged();
    }

    public int getBonusMaxStamina() { return bonusMaxStamina; }
    public void setBonusMaxStamina(int bonusMaxStamina) {
        this.bonusMaxStamina = bonusMaxStamina;
        validateCurrentStats();
        notifyStatusChanged();
    }

    public int getBonusHpRegen() { return bonusHpRegen; }
    public void setBonusHpRegen(int bonusHpRegen) {
        this.bonusHpRegen = bonusHpRegen;
        notifyStatusChanged();
    }

    public int getBonusManaRegen() { return bonusManaRegen; }
    public void setBonusManaRegen(int bonusManaRegen) {
        this.bonusManaRegen = bonusManaRegen;
        notifyStatusChanged();
    }

    public int getBonusStaminaRegen() { return bonusStaminaRegen; }
    public void setBonusStaminaRegen(int bonusStaminaRegen) {
        this.bonusStaminaRegen = bonusStaminaRegen;
        notifyStatusChanged();
    }

    public int getBonusPhysicalDefense() { return bonusPhysicalDefense; }
    public void setBonusPhysicalDefense(int bonusPhysicalDefense) {
        this.bonusPhysicalDefense = bonusPhysicalDefense;
        notifyStatusChanged();
    }

    public int getBonusMagicDefense() { return bonusMagicDefense; }
    public void setBonusMagicDefense(int bonusMagicDefense) {
        this.bonusMagicDefense = bonusMagicDefense;
        notifyStatusChanged();
    }

    public int getBonusPhysicalPower() { return bonusPhysicalPower; }
    public void setBonusPhysicalPower(int bonusPhysicalPower) {
        this.bonusPhysicalPower = bonusPhysicalPower;
        notifyStatusChanged();
    }

    public int getBonusRangedPower() { return bonusRangedPower; }
    public void setBonusRangedPower(int bonusRangedPower) {
        this.bonusRangedPower = bonusRangedPower;
        notifyStatusChanged();
    }

    public int getBonusMagicPower() { return bonusMagicPower; }
    public void setBonusMagicPower(int bonusMagicPower) {
        this.bonusMagicPower = bonusMagicPower;
        notifyStatusChanged();
    }

    public float getBonusCriticalChance() { return bonusCriticalChance; }
    public void setBonusCriticalChance(float bonusCriticalChance) {
        this.bonusCriticalChance = bonusCriticalChance;
        notifyStatusChanged();
    }

    public float getBonusCriticalDamage() { return bonusCriticalDamage; }
    public void setBonusCriticalDamage(float bonusCriticalDamage) {
        this.bonusCriticalDamage = bonusCriticalDamage;
        notifyStatusChanged();
    }

    public float getBonusDodgeChance() { return bonusDodgeChance; }
    public void setBonusDodgeChance(float bonusDodgeChance) {
        this.bonusDodgeChance = bonusDodgeChance;
        notifyStatusChanged();
    }

    public float getBonusAttackSpeed() { return bonusAttackSpeed; }
    public void setBonusAttackSpeed(float bonusAttackSpeed) {
        this.bonusAttackSpeed = bonusAttackSpeed;
        notifyStatusChanged();
    }

    public float getBonusMovementSpeed() { return bonusMovementSpeed; }
    public void setBonusMovementSpeed(float bonusMovementSpeed) {
        this.bonusMovementSpeed = bonusMovementSpeed;
        notifyStatusChanged();
    }

    public float getBonusCooldownReduction() { return bonusCooldownReduction; }
    public void setBonusCooldownReduction(float bonusCooldownReduction) {
        this.bonusCooldownReduction = bonusCooldownReduction;
        notifyStatusChanged();
    }

    public float getBonusLifeSteal() { return bonusLifeSteal; }
    public void setBonusLifeSteal(float bonusLifeSteal) {
        this.bonusLifeSteal = bonusLifeSteal;
        notifyStatusChanged();
    }

    public float getBonusManaSteal() { return bonusManaSteal; }
    public void setBonusManaSteal(float bonusManaSteal) {
        this.bonusManaSteal = bonusManaSteal;
        notifyStatusChanged();
    }

    public float getBonusTenacity() { return bonusTenacity; }
    public void setBonusTenacity(float bonusTenacity) {
        this.bonusTenacity = bonusTenacity;
        notifyStatusChanged();
    }

    public int getBonusLuck() { return bonusLuck; }
    public void setBonusLuck(int bonusLuck) {
        this.bonusLuck = bonusLuck;
        notifyStatusChanged();
    }

    public int getBonusFireResistance() { return bonusFireResistance; }
    public void setBonusFireResistance(int bonusFireResistance) {
        this.bonusFireResistance = bonusFireResistance;
        notifyStatusChanged();
    }

    public int getBonusIceResistance() { return bonusIceResistance; }
    public void setBonusIceResistance(int bonusIceResistance) {
        this.bonusIceResistance = bonusIceResistance;
        notifyStatusChanged();
    }

    public int getBonusLightningResistance() { return bonusLightningResistance; }
    public void setBonusLightningResistance(int bonusLightningResistance) {
        this.bonusLightningResistance = bonusLightningResistance;
        notifyStatusChanged();
    }

    public int getBonusPoisonResistance() { return bonusPoisonResistance; }
    public void setBonusPoisonResistance(int bonusPoisonResistance) {
        this.bonusPoisonResistance = bonusPoisonResistance;
        notifyStatusChanged();
    }

    public int getBonusHolyResistance() { return bonusHolyResistance; }
    public void setBonusHolyResistance(int bonusHolyResistance) {
        this.bonusHolyResistance = bonusHolyResistance;
        notifyStatusChanged();
    }

    public int getBonusDarkResistance() { return bonusDarkResistance; }
    public void setBonusDarkResistance(int bonusDarkResistance) {
        this.bonusDarkResistance = bonusDarkResistance;
        notifyStatusChanged();
    }

    // ==================== GETTERS E SETTERS DE COMBATE ====================

    public CombatStats getCombatStats() { return combatStats; }
    public void setCombatStats(CombatStats combatStats) { this.combatStats = combatStats; }

    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }

    public boolean isAttacking() { return isAttacking; }
    public void setAttacking(boolean attacking) { isAttacking = attacking; }

    public float getAttackTimer() { return attackTimer; }
    public void setAttackTimer(float attackTimer) { this.attackTimer = attackTimer; }

    public float getCurrentAttackCooldown() { return currentAttackCooldown; }
    public void setCurrentAttackCooldown(float currentAttackCooldown) { this.currentAttackCooldown = currentAttackCooldown; }

    // Dash
    public boolean isDashing() { return isDashing; }
    public void setDashing(boolean dashing) { isDashing = dashing; }

    public float getDashTimer() { return dashTimer; }
    public void setDashTimer(float dashTimer) { this.dashTimer = dashTimer; }

    public float getDashStartX() { return dashStartX; }
    public void setDashStartX(float dashStartX) { this.dashStartX = dashStartX; }

    public float getDashStartY() { return dashStartY; }
    public void setDashStartY(float dashStartY) { this.dashStartY = dashStartY; }

    // Chunk e timers
    public Integer getLastChunkX() { return lastChunkX; }
    public void setLastChunkX(Integer lastChunkX) { this.lastChunkX = lastChunkX; }

    public Integer getLastChunkY() { return lastChunkY; }
    public void setLastChunkY(Integer lastChunkY) { this.lastChunkY = lastChunkY; }

    public long getLastSaveTime() { return lastSaveTime; }
    public void setLastSaveTime(long lastSaveTime) { this.lastSaveTime = lastSaveTime; }

    public long getLastMovementSend() { return lastMovementSend; }
    public void setLastMovementSend(long lastMovementSend) { this.lastMovementSend = lastMovementSend; }

    // Callback
    public void setOnStatusChanged(Runnable callback) { this.onStatusChanged = callback; }
    private void notifyStatusChanged() { if (onStatusChanged != null) onStatusChanged.run(); }
    public Runnable getOnStatusChanged() { return onStatusChanged; }

    // Métodos de utilidade
    public float getHpPercentage() { return (float) currentHp / getMaxHp(); }
    public float getManaPercentage() { return (float) currentMana / getMaxMana(); }
    public float getStaminaPercentage() { return (float) currentStamina / getMaxStamina(); }

    public static float getSprintMultiplier() { return SPRINT_MULTIPLIER; }
    public static float getSprintStaminaCost() { return SPRINT_STAMINA_COST; }
    public static int getDashCooldownMs() { return DASH_COOLDOWN_MS; }
    public static int getDashDistance() { return DASH_DISTANCE; }
    public static int getDashStaminaCost() { return DASH_STAMINA_COST; }
    public static float getDashDuration() { return DASH_DURATION; }

    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', lvl=%d, hp=%d/%d, ap=%d, power=%d/%d/%d}",
                id, username, level, currentHp, getMaxHp(), attributePoints,
                getPhysicalPower(), getRangedPower(), getMagicPower());
    }
}