package com.common.sandbox.model;

/**
 * Sistema de Experiência com curva exponencial suave para MMORPG
 * Fórmula: XP necessário = baseXP * (level^expoente)
 *
 * Exemplo: level 1 = 100xp, level 50 = ~1.4M xp, level 100 = ~14M xp
 */
public class ExperienceSystem {

    private static final int BASE_XP = 100;      // XP para level 1
    private static final float EXPONENT = 1.8f;   // Curva (1.5 = fácil, 2.0 = difícil)
    private static final int MAX_LEVEL = 100;

    /**
     * Calcula XP necessário para atingir um determinado level
     */
    public static int getRequiredXP(int level) {
        if (level <= 1) return 0;
        if (level > MAX_LEVEL) return Integer.MAX_VALUE;

        // Fórmula: BASE_XP * (level^EXPONENT)
        return (int) (BASE_XP * Math.pow(level - 1, EXPONENT));
    }

    /**
     * Calcula XP total acumulado até o level
     */
    public static int getTotalXPToLevel(int level) {
        int total = 0;
        for (int i = 2; i <= level; i++) {
            total += getRequiredXP(i);
        }
        return total;
    }

    /**
     * Calcula o level baseado no XP total
     */
    public static int calculateLevel(int totalXP) {
        int level = 1;
        int xpNeeded = getRequiredXP(level + 1);

        while (totalXP >= xpNeeded && level < MAX_LEVEL) {
            level++;
            totalXP -= xpNeeded;
            xpNeeded = getRequiredXP(level + 1);
        }

        return level;
    }

    /**
     * Calcula porcentagem de progressão para o próximo level
     */
    public static float getProgressToNextLevel(int currentXP, int currentLevel) {
        int xpForCurrent = getRequiredXP(currentLevel);
        int xpForNext = getRequiredXP(currentLevel + 1);
        int xpEarnedInLevel = currentXP - xpForCurrent;
        int xpNeededForNext = xpForNext - xpForCurrent;

        if (xpNeededForNext <= 0) return 1.0f;
        return Math.min(1.0f, (float) xpEarnedInLevel / xpNeededForNext);
    }

    /**
     * Adiciona XP e retorna quantos levels up ocorreram
     */
    public static AddXpResult addExperience(Player player, int gainedXP) {
        int oldLevel = player.getLevel();
        int newXP = player.getExperience() + gainedXP;
        int newLevel = calculateLevel(newXP);

        int levelsGained = newLevel - oldLevel;

        if (levelsGained > 0) {
            // Aplicar bônus de level up
            for (int i = 0; i < levelsGained; i++) {
                applyLevelUpBonus(player);
            }
        }

        player.setExperience(newXP);
        player.setLevel(newLevel);

        return new AddXpResult(gainedXP, oldLevel, newLevel, levelsGained);
    }

    /**
     * Aplica bônus de level up
     */
    private static void applyLevelUpBonus(Player player) {
        // Atributos máximos aumentam
        player.setMaxHp(player.getMaxHp() + 10);
        player.setMaxMana(player.getMaxMana() + 5);
        player.setMaxStamina(player.getMaxStamina() + 5);

        // Cura completa ao upar
        player.setCurrentHp(player.getMaxHp());
        player.setCurrentMana(player.getMaxMana());
        player.setCurrentStamina(player.getMaxStamina());

        // Pontos para distribuir
        player.setAttributePoints(player.getAttributePoints() + 3);
        player.setSkillPoints(player.getSkillPoints() + 1);
    }

    /**
     * Calcula XP ganho ao matar um monstro baseado na dificuldade
     */
    public static int calculateMonsterXP(int monsterLevel, int playerLevel) {
        int baseXP = 50;
        float levelDifference = monsterLevel - playerLevel;

        // Bônus/Penalidade baseado na diferença de level
        float multiplier;
        if (levelDifference >= 5) {
            multiplier = 1.5f;  // Bônus para monstros muito mais fortes
        } else if (levelDifference >= 2) {
            multiplier = 1.2f;
        } else if (levelDifference <= -5) {
            multiplier = 0.3f;  // Penalidade para monstros muito fracos
        } else if (levelDifference <= -2) {
            multiplier = 0.7f;
        } else {
            multiplier = 1.0f;
        }

        return (int) (baseXP * multiplier);
    }

    /**
     * Resultado da adição de XP
     */
    public static class AddXpResult {
        public final int xpGained;
        public final int oldLevel;
        public final int newLevel;
        public final int levelsGained;

        public AddXpResult(int xpGained, int oldLevel, int newLevel, int levelsGained) {
            this.xpGained = xpGained;
            this.oldLevel = oldLevel;
            this.newLevel = newLevel;
            this.levelsGained = levelsGained;
        }

        public boolean hasLeveledUp() {
            return levelsGained > 0;
        }
    }

    // Tabela de referência para debug
    public static void printXpTable() {
        System.out.println("=== XP Table (Curve ^" + EXPONENT + ") ===");
        for (int level = 1; level <= 100; level += 10) {
            int total = getTotalXPToLevel(level);
            System.out.printf("Level %d: %,d XP total%n", level, total);
        }
    }
}