package com.common.sandbox.model.player;

import com.common.sandbox.model.item.Inventory;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    // --- DADOS PERSISTENTES ---
    private String id;
    private String username;
    private String email;
    private float x;
    private float y;
    private String direction;

    private boolean isOnline;
    private int level;
    private int experience;
    private int gold;

    private Inventory inventory;

    private int currentHp;
    private int currentMana;
    private int currentStamina;

    // VALORES BASE (persistidos no banco)
    private int baseHp;
    private int baseMana;
    private int baseStamina;

    private transient boolean isDashing;
    private transient float dashTimer;
    private transient float dashStartX;
    private transient float dashStartY;

    private int strength;
    private int agility;
    private int wisdom;

    private int attributePoints;
    private int skillPoints;

    // Regeneracao (persiste)
    private int hpRegenPerSecond = 3;
    private int manaRegenPerSecond = 3;
    private int staminaRegenPerSecond = 5;

    // --- DADOS TEMPORARIOS (NAO PERSISTEM) ---
    private transient long lastRegenTime;
    private transient long lastDashTime;
    private transient long lastMovementSend;
    private transient Integer lastChunkX;
    private transient Integer lastChunkY;
    private transient long lastSaveTime;

    private CombatStats combatStats;
    private transient long lastAttackTime;
    private transient boolean isAttacking;
    private transient float attackTimer;

    // NOVO CAMPO - Cooldown atual recebido do servidor (cliente usa isso)
    private transient float currentAttackCooldown = 1.0f;

    // Callback para notificar quando o status mudar
    private transient Runnable onStatusChanged;

    // --- VALORES FIXOS DO GAME ---
    private static final float BASE_SPEED = 400f;
    private static final float SPRINT_MULTIPLIER = 1.4f;
    private static final float SPRINT_STAMINA_COST = 0.001f;
    private static final int DASH_COOLDOWN_MS = 2000;
    private static final int DASH_DISTANCE = 150;
    private static final int DASH_STAMINA_COST = 20;
    private static final float DASH_DURATION = 0.15f;

    // CONSTANTES PARA BONUS POR ATRIBUTO
    private static final int HP_PER_STRENGTH = 5;
    private static final int MANA_PER_WISDOM = 5;
    private static final int STAMINA_PER_AGILITY = 5;
    private static final int HP_PER_LEVEL = 10;
    private static final int MANA_PER_LEVEL = 5;
    private static final int STAMINA_PER_LEVEL = 5;

    public Player() {
        this.id = "";
        this.username = "";
        this.email = "";
        this.x = 400;
        this.y = 300;
        this.direction = "DOWN";
        this.isOnline = true;
        this.inventory = new Inventory();

        this.level = 1;
        this.experience = 0;
        this.gold = 0;

        this.strength = 5;
        this.agility = 5;
        this.wisdom = 5;

        this.baseHp = 100;
        this.baseMana = 50;
        this.baseStamina = 100;

        this.currentHp = getMaxHp();
        this.currentMana = getMaxMana();
        this.currentStamina = getMaxStamina();

        this.attributePoints = 0;
        this.skillPoints = 0;

        this.combatStats = new CombatStats();
        this.lastAttackTime = 0;
        this.isAttacking = false;
        this.attackTimer = 0;
        this.currentAttackCooldown = 1.0f;

        long now = System.currentTimeMillis();
        this.lastRegenTime = now;
        this.lastDashTime = 0;
        this.lastMovementSend = 0;
        this.lastSaveTime = now;
    }

    // METODOS DE CALCULO DE MAX (calculados em tempo real)
    public int getMaxHp() {
        return baseHp + (level - 1) * HP_PER_LEVEL + strength * HP_PER_STRENGTH;
    }

    public int getMaxMana() {
        return baseMana + (level - 1) * MANA_PER_LEVEL + wisdom * MANA_PER_WISDOM;
    }

    public int getMaxStamina() {
        return baseStamina + (level - 1) * STAMINA_PER_LEVEL + agility * STAMINA_PER_AGILITY;
    }

    // GETTERS E SETTERS DOS VALORES BASE
    public int getBaseHp() { return baseHp; }
    public void setBaseHp(int baseHp) { this.baseHp = baseHp; }

    public int getBaseMana() { return baseMana; }
    public void setBaseMana(int baseMana) { this.baseMana = baseMana; }

    public int getBaseStamina() { return baseStamina; }
    public void setBaseStamina(int baseStamina) { this.baseStamina = baseStamina; }

    // Getters e Setters padrao
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

    public Inventory getInventory() { return inventory; }
    public void setInventory(Inventory inventory) { this.inventory = inventory; }

    public int getLevel() { return level; }
    public void setLevel(int level) {
        this.level = level;
        validateCurrentStats();
        notifyStatusChanged();
    }

    public int getExperience() { return experience; }
    public void setExperience(int experience) {
        if (this.experience != experience) {
            this.experience = experience;
            notifyStatusChanged();
        }
    }

    public int getGold() { return gold; }
    public void setGold(int gold) {
        if (this.gold != gold) {
            this.gold = gold;
            notifyStatusChanged();
        }
    }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) {
        int newValue = Math.min(getMaxHp(), Math.max(0, currentHp));
        if (this.currentHp != newValue) {
            this.currentHp = newValue;
            notifyStatusChanged();
        }
    }

    public int getCurrentMana() { return currentMana; }
    public void setCurrentMana(int currentMana) {
        int newValue = Math.min(getMaxMana(), Math.max(0, currentMana));
        if (this.currentMana != newValue) {
            this.currentMana = newValue;
            notifyStatusChanged();
        }
    }

    public int getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(int currentStamina) {
        int newValue = Math.min(getMaxStamina(), Math.max(0, currentStamina));
        if (this.currentStamina != newValue) {
            this.currentStamina = newValue;
            notifyStatusChanged();
        }
    }

    public int getStrength() { return strength; }
    public void setStrength(int strength) {
        if (this.strength != strength) {
            this.strength = strength;
            validateCurrentStats();
            notifyStatusChanged();
        }
    }

    public int getAgility() { return agility; }
    public void setAgility(int agility) {
        if (this.agility != agility) {
            this.agility = agility;
            validateCurrentStats();
            notifyStatusChanged();
        }
    }

    public int getWisdom() { return wisdom; }
    public void setWisdom(int wisdom) {
        if (this.wisdom != wisdom) {
            this.wisdom = wisdom;
            validateCurrentStats();
            notifyStatusChanged();
        }
    }

    public int getAttributePoints() { return attributePoints; }
    public void setAttributePoints(int attributePoints) {
        if (this.attributePoints != attributePoints) {
            this.attributePoints = attributePoints;
            notifyStatusChanged();
        }
    }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) {
        if (this.skillPoints != skillPoints) {
            this.skillPoints = skillPoints;
            notifyStatusChanged();
        }
    }

    public int getHpRegenPerSecond() { return hpRegenPerSecond; }
    public void setHpRegenPerSecond(int hpRegenPerSecond) { this.hpRegenPerSecond = hpRegenPerSecond; }

    public int getManaRegenPerSecond() { return manaRegenPerSecond; }
    public void setManaRegenPerSecond(int manaRegenPerSecond) { this.manaRegenPerSecond = manaRegenPerSecond; }

    public int getStaminaRegenPerSecond() { return staminaRegenPerSecond; }
    public void setStaminaRegenPerSecond(int staminaRegenPerSecond) { this.staminaRegenPerSecond = staminaRegenPerSecond; }

    public Integer getLastChunkX() { return lastChunkX; }
    public void setLastChunkX(Integer lastChunkX) { this.lastChunkX = lastChunkX; }

    public Integer getLastChunkY() { return lastChunkY; }
    public void setLastChunkY(Integer lastChunkY) { this.lastChunkY = lastChunkY; }

    public long getLastSaveTime() { return lastSaveTime; }
    public void setLastSaveTime(long lastSaveTime) { this.lastSaveTime = lastSaveTime; }

    public long getLastMovementSend() { return lastMovementSend; }
    public void setLastMovementSend(long lastMovementSend) { this.lastMovementSend = lastMovementSend; }

    // Cooldown do ataque (valor recebido do servidor)
    public float getCurrentAttackCooldown() {
        return currentAttackCooldown;
    }

    public void setCurrentAttackCooldown(float cooldown) {
        this.currentAttackCooldown = cooldown;
    }

    // Callback para notificar mudancas de status
    public void setOnStatusChanged(Runnable callback) {
        this.onStatusChanged = callback;
    }

    private void notifyStatusChanged() {
        if (onStatusChanged != null) {
            onStatusChanged.run();
        }
    }

    // VALORES FIXOS (getters estaticos)
    public static float getBaseSpeed() { return BASE_SPEED; }
    public static float getSprintMultiplier() { return SPRINT_MULTIPLIER; }
    public static float getSprintStaminaCost() { return SPRINT_STAMINA_COST; }
    public static int getDashCooldownMs() { return DASH_COOLDOWN_MS; }
    public static int getDashDistance() { return DASH_DISTANCE; }
    public static int getDashStaminaCost() { return DASH_STAMINA_COST; }
    public static float getDashDuration() { return DASH_DURATION; }

    public boolean isDashing() { return isDashing; }
    public void setDashing(boolean dashing) { isDashing = dashing; }
    public float getDashTimer() { return dashTimer; }
    public void setDashTimer(float dashTimer) { this.dashTimer = dashTimer; }
    public float getDashStartX() { return dashStartX; }
    public void setDashStartX(float dashStartX) { this.dashStartX = dashStartX; }
    public float getDashStartY() { return dashStartY; }
    public void setDashStartY(float dashStartY) { this.dashStartY = dashStartY; }

    public CombatStats getCombatStats() { return combatStats; }
    public void setCombatStats(CombatStats combatStats) { this.combatStats = combatStats; }
    public long getLastAttackTime() { return lastAttackTime; }
    public void setLastAttackTime(long lastAttackTime) { this.lastAttackTime = lastAttackTime; }
    public boolean isAttacking() { return isAttacking; }
    public void setAttacking(boolean attacking) { isAttacking = attacking; }
    public float getAttackTimer() { return attackTimer; }
    public void setAttackTimer(float attackTimer) { this.attackTimer = attackTimer; }

    // METODO PARA VALIDAR QUE CURRENT NAO ULTRAPASSA MAX
    private void validateCurrentStats() {
        int maxHp = getMaxHp();
        int maxMana = getMaxMana();
        int maxStamina = getMaxStamina();

        if (currentHp > maxHp) currentHp = maxHp;
        if (currentMana > maxMana) currentMana = maxMana;
        if (currentStamina > maxStamina) currentStamina = maxStamina;
    }

    // METODOS DE UTILIDADE
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

    public void updateRegeneration(float delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRegenTime;

        if (elapsed >= 1000) {
            int seconds = (int) (elapsed / 1000);
            int maxHp = getMaxHp();
            int maxMana = getMaxMana();
            int maxStamina = getMaxStamina();

            boolean changed = false;

            if (currentHp < maxHp) {
                currentHp = Math.min(maxHp, currentHp + (hpRegenPerSecond * seconds));
                changed = true;
            }
            if (currentMana < maxMana) {
                currentMana = Math.min(maxMana, currentMana + (manaRegenPerSecond * seconds));
                changed = true;
            }
            if (currentStamina < maxStamina) {
                currentStamina = Math.min(maxStamina, currentStamina + (staminaRegenPerSecond * seconds));
                changed = true;
            }

            if (changed) {
                notifyStatusChanged();
            }

            lastRegenTime = now - (elapsed % 1000);
        }
    }

    public float getHpPercentage() {
        return (float) currentHp / getMaxHp();
    }

    public float getManaPercentage() {
        return (float) currentMana / getMaxMana();
    }

    public float getStaminaPercentage() {
        return (float) currentStamina / getMaxStamina();
    }

    public int getXpForNextLevel() {
        return ExperienceSystem.getRequiredXP(level + 1);
    }

    public float getXpProgress() {
        return ExperienceSystem.getProgressToNextLevel(experience, level);
    }

    public boolean addExperience(int amount) {
        ExperienceSystem.AddXpResult result = ExperienceSystem.addExperience(this, amount);
        if (result.hasLeveledUp()) {
            validateCurrentStats();
            notifyStatusChanged();
            return true;
        }
        return false;
    }

    public boolean canAttack() {
        long now = System.currentTimeMillis();
        float cooldownSeconds = getCurrentAttackCooldown();
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

    public void updateCombatStatsFromEquipment() {
        if (combatStats == null) combatStats = new CombatStats();

        int weaponBonus = 0;
        String equippedWeapon = inventory != null ? inventory.getEquipped().get("weapon") : null;

        if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
            if (equippedWeapon.contains("sword")) weaponBonus = 5;
            else if (equippedWeapon.contains("dagger")) weaponBonus = 3;
            else if (equippedWeapon.contains("axe")) weaponBonus = 8;
            else if (equippedWeapon.contains("hammer")) weaponBonus = 10;
            else if (equippedWeapon.contains("bow")) weaponBonus = 4;
        }

        combatStats.setWeaponDamageBonus(weaponBonus);
        combatStats.setStrengthBonus(strength / 2);
    }

    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', level=%d, hp=%d/%d, stamina=%d/%d}",
                id, username, level, currentHp, getMaxHp(), currentStamina, getMaxStamina());
    }
}