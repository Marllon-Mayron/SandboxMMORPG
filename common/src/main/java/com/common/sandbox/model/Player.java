package com.common.sandbox.model;

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

    private int maxHp;
    private int maxMana;
    private int maxStamina;

    private transient boolean isDashing;
    private transient float dashTimer;
    private transient float dashStartX;
    private transient float dashStartY;

    private int strength;
    private int agility;
    private int wisdom;

    private int attributePoints;
    private int skillPoints;

    // Regeneração (persiste)
    private int hpRegenPerSecond = 3;
    private int manaRegenPerSecond = 3;
    private int staminaRegenPerSecond = 5;

    // --- DADOS TEMPORÁRIOS (NÃO PERSISTEM) ---
    private transient long lastRegenTime;
    private transient long lastDashTime;
    private transient long lastMovementSend;
    private transient Integer lastChunkX;
    private transient Integer lastChunkY;
    private transient long lastSaveTime;

    private CombatStats combatStats;
    private transient long lastAttackTime;  // Para cooldown
    private transient boolean isAttacking;
    private transient float attackTimer;

    // --- VALORES FIXOS DO GAME (não persistem, iguais para todos) ---
    private static final float BASE_SPEED = 400f;
    private static final float SPRINT_MULTIPLIER = 1.4f;
    private static final float SPRINT_STAMINA_COST = 0.001f;
    private static final int DASH_COOLDOWN_MS = 2000;
    private static final int DASH_DISTANCE = 150;
    private static final int DASH_STAMINA_COST = 20;
    private static final float DASH_DURATION = 0.15f;

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

        recalculateMaxStats();

        this.attributePoints = 0;
        this.skillPoints = 0;

        this.combatStats = new CombatStats();
        this.lastAttackTime = 0;
        this.isAttacking = false;
        this.attackTimer = 0;

        long now = System.currentTimeMillis();
        this.lastRegenTime = now;
        this.lastDashTime = 0;
        this.lastMovementSend = 0;
        this.lastSaveTime = now;
    }

    // Getters e Setters padrão...
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
    public void setLevel(int level) { this.level = level; recalculateMaxStats(); }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = Math.min(maxHp, Math.max(0, currentHp)); }

    public int getCurrentMana() { return currentMana; }
    public void setCurrentMana(int currentMana) { this.currentMana = Math.min(maxMana, Math.max(0, currentMana)); }

    public int getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(int currentStamina) { this.currentStamina = Math.min(maxStamina, Math.max(0, currentStamina)); }

    public int getMaxHp() { return maxHp; }
    public void setMaxHp(int maxHp) { this.maxHp = maxHp; }

    public int getMaxMana() { return maxMana; }
    public void setMaxMana(int maxMana) { this.maxMana = maxMana; }

    public int getMaxStamina() { return maxStamina; }
    public void setMaxStamina(int maxStamina) { this.maxStamina = maxStamina; }

    public int getStrength() { return strength; }
    public void setStrength(int strength) { this.strength = strength; recalculateMaxStats(); }

    public int getAgility() { return agility; }
    public void setAgility(int agility) { this.agility = agility; recalculateMaxStats(); }

    public int getWisdom() { return wisdom; }
    public void setWisdom(int wisdom) { this.wisdom = wisdom; recalculateMaxStats(); }

    public int getAttributePoints() { return attributePoints; }
    public void setAttributePoints(int attributePoints) { this.attributePoints = attributePoints; }

    public int getSkillPoints() { return skillPoints; }
    public void setSkillPoints(int skillPoints) { this.skillPoints = skillPoints; }

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

    // --- VALORES FIXOS (getters estáticos) ---
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

    // --- MÉTODOS DE UTILIDADE ---

    public void recalculateMaxStats() {
        this.maxHp = 100 + (level - 1) * 10 + strength * 5;
        this.maxMana = 50 + (level - 1) * 5 + wisdom * 5;
        this.maxStamina = 100 + (level - 1) * 5 + agility * 5;

        if (currentHp > maxHp) currentHp = maxHp;
        if (currentMana > maxMana) currentMana = maxMana;
        if (currentStamina > maxStamina) currentStamina = maxStamina;
    }

    public boolean canDash() {
        long now = System.currentTimeMillis();
        return (now - lastDashTime) >= DASH_COOLDOWN_MS && currentStamina >= DASH_STAMINA_COST;
    }

    public boolean executeDash() {
        if (!canDash()) return false;

        lastDashTime = System.currentTimeMillis();
        currentStamina -= DASH_STAMINA_COST;
        return true;
    }

    public boolean consumeStaminaForSprint(float delta) {
        float cost = SPRINT_STAMINA_COST * delta;  // sem *60
        System.out.println("Consume stamina - delta: {"+ delta+ "}, cost: { " +cost+ "}, current stamina: {"+currentStamina+"}");

        if (currentStamina >= cost) {
            currentStamina -= cost;
            return true;
        }
        return false;
    }

    public void updateRegeneration(float delta) {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRegenTime;

        if (elapsed >= 1000) {
            int seconds = (int) (elapsed / 1000);

            if (currentHp < maxHp) {
                currentHp = Math.min(maxHp, currentHp + (hpRegenPerSecond * seconds));
            }
            if (currentMana < maxMana) {
                currentMana = Math.min(maxMana, currentMana + (manaRegenPerSecond * seconds));
            }
            if (currentStamina < maxStamina) {
                currentStamina = Math.min(maxStamina, currentStamina + (staminaRegenPerSecond * seconds));
            }

            lastRegenTime = now - (elapsed % 1000);
        }
    }

    public float getHpPercentage() { return (float) currentHp / maxHp; }
    public float getManaPercentage() { return (float) currentMana / maxMana; }
    public float getStaminaPercentage() { return (float) currentStamina / maxStamina; }

    public int getXpForNextLevel() {
        return ExperienceSystem.getRequiredXP(level + 1);
    }

    public float getXpProgress() {
        return ExperienceSystem.getProgressToNextLevel(experience, level);
    }

    public boolean addExperience(int amount) {
        ExperienceSystem.AddXpResult result = ExperienceSystem.addExperience(this, amount);
        if (result.hasLeveledUp()) {
            recalculateMaxStats();
            return true;
        }
        return false;
    }

    // Método para verificar se pode atacar (cooldown global 2 segundos)
    public boolean canAttack() {
        long now = System.currentTimeMillis();
        return (now - lastAttackTime) >= 2000; // 2 segundos cooldown
    }

    // Método para executar ataque
    public boolean executeAttack() {
        if (!canAttack()) return false;
        lastAttackTime = System.currentTimeMillis();
        isAttacking = true;
        attackTimer = 0.3f; // duração da animação
        return true;
    }

    // Método para atualizar estado do ataque (chamar no update)
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
                id, username, level, currentHp, maxHp, currentStamina, maxStamina);
    }
}