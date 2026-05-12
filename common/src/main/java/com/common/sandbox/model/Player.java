package com.common.sandbox.model;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private float x;
    private float y;
    private String direction;
    private Integer lastChunkX;
    private Integer lastChunkY;

    // NOVOS CAMPOS
    private boolean isOnline;
    private int level;
    private int experience;
    private int gold;

    // Status atuais
    private int currentHp;
    private int currentMana;
    private int currentStamina;

    // Status máximos
    private int maxHp;
    private int maxMana;
    private int maxStamina;

    // Atributos
    private int strength;   // Força
    private int agility;    // Agilidade
    private int wisdom;     // Sabedoria

    // Pontos para gastar
    private int attributePoints;
    private int skillPoints;

    private transient long lastSaveTime;

    // Construtor padrão (OBRIGATÓRIO para Kryo)
    public Player() {
        this.id = "";
        this.username = "";
        this.email = "";
        this.x = 400;
        this.y = 300;
        this.direction = "DOWN";
        this.isOnline = true;

        // Inicializar atributos
        this.level = 1;
        this.experience = 0;
        this.gold = 0;

        this.strength = 5;
        this.agility = 5;
        this.wisdom = 5;

        this.maxHp = 100 + (level - 1) * 10 + strength * 5;
        this.maxMana = 50 + (level - 1) * 5 + wisdom * 5;
        this.maxStamina = 100 + (level - 1) * 5 + agility * 5;

        this.currentHp = this.maxHp;
        this.currentMana = this.maxMana;
        this.currentStamina = this.maxStamina;

        this.attributePoints = 0;
        this.skillPoints = 0;
    }

    public Player(String id, String username, String email) {
        this();
        this.id = id;
        this.username = username;
        this.email = email;
        recalculateMaxStats();
    }

    /**
     * Recalcula stats máximos baseado no level e atributos
     */
    public void recalculateMaxStats() {
        this.maxHp = 100 + (level - 1) * 10 + strength * 5;
        this.maxMana = 50 + (level - 1) * 5 + wisdom * 5;
        this.maxStamina = 100 + (level - 1) * 5 + agility * 5;

        // Garantir que stats atuais não ultrapassem os máximos
        if (currentHp > maxHp) currentHp = maxHp;
        if (currentMana > maxMana) currentMana = maxMana;
        if (currentStamina > maxStamina) currentStamina = maxStamina;
    }

    /**
     * Adiciona XP e verifica level up
     */
    public boolean addExperience(int amount) {
        ExperienceSystem.AddXpResult result = ExperienceSystem.addExperience(this, amount);
        if (result.hasLeveledUp()) {
            recalculateMaxStats();
            return true;
        }
        return false;
    }

    /**
     * Gasta um ponto de atributo
     */
    public boolean spendAttributePoint(String attribute, int points) {
        if (attributePoints < points) return false;

        switch (attribute.toLowerCase()) {
            case "strength":
                this.strength += points;
                break;
            case "agility":
                this.agility += points;
                break;
            case "wisdom":
                this.wisdom += points;
                break;
            default:
                return false;
        }

        this.attributePoints -= points;
        recalculateMaxStats();
        return true;
    }

    /**
     * Verifica se o jogador está vivo
     */
    public boolean isAlive() {
        return currentHp > 0;
    }

    /**
     * Cura o jogador
     */
    public void heal(int amount) {
        currentHp = Math.min(maxHp, currentHp + amount);
    }

    /**
     * Restaura mana
     */
    public void restoreMana(int amount) {
        currentMana = Math.min(maxMana, currentMana + amount);
    }

    /**
     * Restaura stamina
     */
    public void restoreStamina(int amount) {
        currentStamina = Math.min(maxStamina, currentStamina + amount);
    }

    /**
     * Causa dano ao jogador
     */
    public int takeDamage(int damage) {
        int actualDamage = Math.min(currentHp, Math.max(0, damage));
        currentHp -= actualDamage;
        return actualDamage;
    }

    /**
     * Calcula porcentagem de HP
     */
    public float getHpPercentage() {
        return (float) currentHp / maxHp;
    }

    /**
     * Calcula porcentagem de Mana
     */
    public float getManaPercentage() {
        return (float) currentMana / maxMana;
    }

    /**
     * Calcula porcentagem de Stamina
     */
    public float getStaminaPercentage() {
        return (float) currentStamina / maxStamina;
    }

    /**
     * Calcula XP para próximo level
     */
    public int getXpForNextLevel() {
        return ExperienceSystem.getRequiredXP(level + 1);
    }

    /**
     * Calcula progresso para próximo level (0-1)
     */
    public float getXpProgress() {
        return ExperienceSystem.getProgressToNextLevel(experience, level);
    }

    // Getters e Setters

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

    public Integer getLastChunkX() { return lastChunkX; }
    public void setLastChunkX(Integer lastChunkX) { this.lastChunkX = lastChunkX; }

    public Integer getLastChunkY() { return lastChunkY; }
    public void setLastChunkY(Integer lastChunkY) { this.lastChunkY = lastChunkY; }

    public long getLastSaveTime() { return lastSaveTime; }
    public void setLastSaveTime(long lastSaveTime) { this.lastSaveTime = lastSaveTime; }

    // Novos Getters/Setters
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = level; }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = experience; }

    public int getGold() { return gold; }
    public void setGold(int gold) { this.gold = gold; }

    public int getCurrentHp() { return currentHp; }
    public void setCurrentHp(int currentHp) { this.currentHp = Math.min(maxHp, currentHp); }

    public int getCurrentMana() { return currentMana; }
    public void setCurrentMana(int currentMana) { this.currentMana = Math.min(maxMana, currentMana); }

    public int getCurrentStamina() { return currentStamina; }
    public void setCurrentStamina(int currentStamina) { this.currentStamina = Math.min(maxStamina, currentStamina); }

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

    @Override
    public String toString() {
        return String.format("Player{id='%s', name='%s', level=%d, hp=%d/%d, mana=%d/%d, gold=%d}",
                id, username, level, currentHp, maxHp, currentMana, maxMana, gold);
    }
}