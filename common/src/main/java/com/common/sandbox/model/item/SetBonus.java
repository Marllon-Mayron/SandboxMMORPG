package com.common.sandbox.model.item;

import com.common.sandbox.model.player.Player;
import java.io.Serializable;

/**
 * Bônus concedido por usar um conjunto completo de armadura (4 peças)
 */
public class SetBonus implements Serializable {
    private static final long serialVersionUID = 1L;

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

    // ==================== CONSTRUTOR PRIVADO ====================

    private SetBonus() {}

    // ==================== MÉTODOS ESTÁTICOS FACTORY ====================

    /**
     * Cria bônus para conjunto de Guerreiro/Tank
     */
    public static SetBonus warrior(int bonusMaxHp, int bonusPhysicalDefense,
                                   int bonusPhysicalPower, float bonusMovementSpeed, float bonusCriticalChance) {
        SetBonus bonus = new SetBonus();
        bonus.bonusMaxHp = bonusMaxHp;
        bonus.bonusPhysicalDefense = bonusPhysicalDefense;
        bonus.bonusPhysicalPower = bonusPhysicalPower;
        bonus.bonusMovementSpeed = bonusMovementSpeed;
        bonus.bonusCriticalChance = bonusCriticalChance;
        return bonus;
    }

    /**
     * Cria bônus para conjunto de Mago
     */
    public static SetBonus mage(int bonusMaxMana, int bonusMagicDefense,
                                int bonusMagicPower, float bonusCooldownReduction, float bonusCriticalChance) {
        SetBonus bonus = new SetBonus();
        bonus.bonusMaxMana = bonusMaxMana;
        bonus.bonusMagicDefense = bonusMagicDefense;
        bonus.bonusMagicPower = bonusMagicPower;
        bonus.bonusCooldownReduction = bonusCooldownReduction;
        bonus.bonusCriticalChance = bonusCriticalChance;
        return bonus;
    }

    /**
     * Cria bônus para conjunto de Arqueiro
     */
    public static SetBonus archer(int bonusMaxStamina, int bonusRangedPower,
                                  float bonusAttackSpeed, float bonusMovementSpeed, float bonusCriticalChance) {
        SetBonus bonus = new SetBonus();
        bonus.bonusMaxStamina = bonusMaxStamina;
        bonus.bonusRangedPower = bonusRangedPower;
        bonus.bonusAttackSpeed = bonusAttackSpeed;
        bonus.bonusMovementSpeed = bonusMovementSpeed;
        bonus.bonusCriticalChance = bonusCriticalChance;
        return bonus;
    }

    /**
     * Cria bônus customizado
     */
    public static SetBonus custom(int bonusMaxHp, int bonusMaxMana, int bonusMaxStamina,
                                  int bonusPhysicalDefense, int bonusMagicDefense,
                                  int bonusPhysicalPower, int bonusRangedPower, int bonusMagicPower,
                                  float bonusCriticalChance, float bonusCriticalDamage, float bonusDodgeChance,
                                  float bonusAttackSpeed, float bonusMovementSpeed,
                                  float bonusCooldownReduction, float bonusLifeSteal, float bonusManaSteal, float bonusTenacity,
                                  int bonusLuck) {
        SetBonus bonus = new SetBonus();
        bonus.bonusMaxHp = bonusMaxHp;
        bonus.bonusMaxMana = bonusMaxMana;
        bonus.bonusMaxStamina = bonusMaxStamina;
        bonus.bonusPhysicalDefense = bonusPhysicalDefense;
        bonus.bonusMagicDefense = bonusMagicDefense;
        bonus.bonusPhysicalPower = bonusPhysicalPower;
        bonus.bonusRangedPower = bonusRangedPower;
        bonus.bonusMagicPower = bonusMagicPower;
        bonus.bonusCriticalChance = bonusCriticalChance;
        bonus.bonusCriticalDamage = bonusCriticalDamage;
        bonus.bonusDodgeChance = bonusDodgeChance;
        bonus.bonusAttackSpeed = bonusAttackSpeed;
        bonus.bonusMovementSpeed = bonusMovementSpeed;
        bonus.bonusCooldownReduction = bonusCooldownReduction;
        bonus.bonusLifeSteal = bonusLifeSteal;
        bonus.bonusManaSteal = bonusManaSteal;
        bonus.bonusTenacity = bonusTenacity;
        bonus.bonusLuck = bonusLuck;
        return bonus;
    }

    // ==================== GETTERS ====================

    public int getBonusMaxHp() { return bonusMaxHp; }
    public int getBonusMaxMana() { return bonusMaxMana; }
    public int getBonusMaxStamina() { return bonusMaxStamina; }
    public int getBonusHpRegen() { return bonusHpRegen; }
    public int getBonusManaRegen() { return bonusManaRegen; }
    public int getBonusStaminaRegen() { return bonusStaminaRegen; }
    public int getBonusPhysicalDefense() { return bonusPhysicalDefense; }
    public int getBonusMagicDefense() { return bonusMagicDefense; }
    public int getBonusPhysicalPower() { return bonusPhysicalPower; }
    public int getBonusRangedPower() { return bonusRangedPower; }
    public int getBonusMagicPower() { return bonusMagicPower; }
    public float getBonusCriticalChance() { return bonusCriticalChance; }
    public float getBonusCriticalDamage() { return bonusCriticalDamage; }
    public float getBonusDodgeChance() { return bonusDodgeChance; }
    public float getBonusAttackSpeed() { return bonusAttackSpeed; }
    public float getBonusMovementSpeed() { return bonusMovementSpeed; }
    public float getBonusCooldownReduction() { return bonusCooldownReduction; }
    public float getBonusLifeSteal() { return bonusLifeSteal; }
    public float getBonusManaSteal() { return bonusManaSteal; }
    public float getBonusTenacity() { return bonusTenacity; }
    public int getBonusLuck() { return bonusLuck; }
    public int getBonusFireResistance() { return bonusFireResistance; }
    public int getBonusIceResistance() { return bonusIceResistance; }
    public int getBonusLightningResistance() { return bonusLightningResistance; }
    public int getBonusPoisonResistance() { return bonusPoisonResistance; }
    public int getBonusHolyResistance() { return bonusHolyResistance; }
    public int getBonusDarkResistance() { return bonusDarkResistance; }

    // ==================== MÉTODOS DE APLICAÇÃO ====================

    public void applyToPlayer(Player player) {
        player.setBonusMaxHp(player.getBonusMaxHp() + bonusMaxHp);
        player.setBonusMaxMana(player.getBonusMaxMana() + bonusMaxMana);
        player.setBonusMaxStamina(player.getBonusMaxStamina() + bonusMaxStamina);
        player.setBonusHpRegen(player.getBonusHpRegen() + bonusHpRegen);
        player.setBonusManaRegen(player.getBonusManaRegen() + bonusManaRegen);
        player.setBonusStaminaRegen(player.getBonusStaminaRegen() + bonusStaminaRegen);
        player.setBonusPhysicalDefense(player.getBonusPhysicalDefense() + bonusPhysicalDefense);
        player.setBonusMagicDefense(player.getBonusMagicDefense() + bonusMagicDefense);
        player.setBonusPhysicalPower(player.getBonusPhysicalPower() + bonusPhysicalPower);
        player.setBonusRangedPower(player.getBonusRangedPower() + bonusRangedPower);
        player.setBonusMagicPower(player.getBonusMagicPower() + bonusMagicPower);
        player.setBonusCriticalChance(player.getBonusCriticalChance() + bonusCriticalChance);
        player.setBonusCriticalDamage(player.getBonusCriticalDamage() + bonusCriticalDamage);
        player.setBonusDodgeChance(player.getBonusDodgeChance() + bonusDodgeChance);
        player.setBonusAttackSpeed(player.getBonusAttackSpeed() + bonusAttackSpeed);
        player.setBonusMovementSpeed(player.getBonusMovementSpeed() + bonusMovementSpeed);
        player.setBonusCooldownReduction(player.getBonusCooldownReduction() + bonusCooldownReduction);
        player.setBonusLifeSteal(player.getBonusLifeSteal() + bonusLifeSteal);
        player.setBonusManaSteal(player.getBonusManaSteal() + bonusManaSteal);
        player.setBonusTenacity(player.getBonusTenacity() + bonusTenacity);
        player.setBonusLuck(player.getBonusLuck() + bonusLuck);
        player.setBonusFireResistance(player.getBonusFireResistance() + bonusFireResistance);
        player.setBonusIceResistance(player.getBonusIceResistance() + bonusIceResistance);
        player.setBonusLightningResistance(player.getBonusLightningResistance() + bonusLightningResistance);
        player.setBonusPoisonResistance(player.getBonusPoisonResistance() + bonusPoisonResistance);
        player.setBonusHolyResistance(player.getBonusHolyResistance() + bonusHolyResistance);
        player.setBonusDarkResistance(player.getBonusDarkResistance() + bonusDarkResistance);
    }

    public void removeFromPlayer(Player player) {
        player.setBonusMaxHp(player.getBonusMaxHp() - bonusMaxHp);
        player.setBonusMaxMana(player.getBonusMaxMana() - bonusMaxMana);
        player.setBonusMaxStamina(player.getBonusMaxStamina() - bonusMaxStamina);
        player.setBonusHpRegen(player.getBonusHpRegen() - bonusHpRegen);
        player.setBonusManaRegen(player.getBonusManaRegen() - bonusManaRegen);
        player.setBonusStaminaRegen(player.getBonusStaminaRegen() - bonusStaminaRegen);
        player.setBonusPhysicalDefense(player.getBonusPhysicalDefense() - bonusPhysicalDefense);
        player.setBonusMagicDefense(player.getBonusMagicDefense() - bonusMagicDefense);
        player.setBonusPhysicalPower(player.getBonusPhysicalPower() - bonusPhysicalPower);
        player.setBonusRangedPower(player.getBonusRangedPower() - bonusRangedPower);
        player.setBonusMagicPower(player.getBonusMagicPower() - bonusMagicPower);
        player.setBonusCriticalChance(player.getBonusCriticalChance() - bonusCriticalChance);
        player.setBonusCriticalDamage(player.getBonusCriticalDamage() - bonusCriticalDamage);
        player.setBonusDodgeChance(player.getBonusDodgeChance() - bonusDodgeChance);
        player.setBonusAttackSpeed(player.getBonusAttackSpeed() - bonusAttackSpeed);
        player.setBonusMovementSpeed(player.getBonusMovementSpeed() - bonusMovementSpeed);
        player.setBonusCooldownReduction(player.getBonusCooldownReduction() - bonusCooldownReduction);
        player.setBonusLifeSteal(player.getBonusLifeSteal() - bonusLifeSteal);
        player.setBonusManaSteal(player.getBonusManaSteal() - bonusManaSteal);
        player.setBonusTenacity(player.getBonusTenacity() - bonusTenacity);
        player.setBonusLuck(player.getBonusLuck() - bonusLuck);
        player.setBonusFireResistance(player.getBonusFireResistance() - bonusFireResistance);
        player.setBonusIceResistance(player.getBonusIceResistance() - bonusIceResistance);
        player.setBonusLightningResistance(player.getBonusLightningResistance() - bonusLightningResistance);
        player.setBonusPoisonResistance(player.getBonusPoisonResistance() - bonusPoisonResistance);
        player.setBonusHolyResistance(player.getBonusHolyResistance() - bonusHolyResistance);
        player.setBonusDarkResistance(player.getBonusDarkResistance() - bonusDarkResistance);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("SetBonus{");
        if (bonusMaxHp > 0) sb.append("+").append(bonusMaxHp).append(" HP ");
        if (bonusMaxMana > 0) sb.append("+").append(bonusMaxMana).append(" Mana ");
        if (bonusMaxStamina > 0) sb.append("+").append(bonusMaxStamina).append(" Stamina ");
        if (bonusPhysicalDefense > 0) sb.append("+").append(bonusPhysicalDefense).append(" DefF ");
        if (bonusMagicDefense > 0) sb.append("+").append(bonusMagicDefense).append(" DefM ");
        if (bonusPhysicalPower > 0) sb.append("+").append(bonusPhysicalPower).append(" DanoF ");
        if (bonusRangedPower > 0) sb.append("+").append(bonusRangedPower).append(" DanoR ");
        if (bonusMagicPower > 0) sb.append("+").append(bonusMagicPower).append(" DanoM ");
        if (bonusCriticalChance > 0) sb.append("+").append((int)(bonusCriticalChance * 100)).append("% Crit ");
        if (bonusMovementSpeed > 0) sb.append("+").append((int)bonusMovementSpeed).append(" Vel ");
        if (bonusCooldownReduction > 0) sb.append("+").append((int)(bonusCooldownReduction * 100)).append("% CDR ");
        if (bonusAttackSpeed > 0) sb.append("+").append((int)(bonusAttackSpeed * 100)).append("% AS ");
        sb.append("}");
        return sb.toString();
    }
}