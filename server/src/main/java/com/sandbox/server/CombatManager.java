package com.sandbox.server;

import com.common.sandbox.model.combat.AttackDefinition;
import com.common.sandbox.model.combat.AttackResult;
import com.common.sandbox.model.enums.AttackType;
import com.common.sandbox.model.item.ItemDefinition;
import com.common.sandbox.model.player.CombatStats;
import com.common.sandbox.model.player.Player;
import com.common.sandbox.network.packets.chat.ChatMessage;
import com.common.sandbox.network.packets.player.PlayerStatePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CombatManager {
    private static final Logger logger = LoggerFactory.getLogger(CombatManager.class);
    private static CombatManager instance;

    private CombatManager() {}

    public static synchronized CombatManager getInstance() {
        if (instance == null) {
            instance = new CombatManager();
        }
        return instance;
    }

    public AttackResult processAttack(Player attacker, Player target, AttackDefinition attackDef) {
        if (attacker == null || target == null) {
            return new AttackResult(false, 0, false, false, null, null, 0, AttackType.MELEE_SWORD, 0, 0);
        }

        // Obter o dano base da arma
        int baseDamage = getWeaponBaseDamage(attacker, attackDef);

        // Obter o bônus percentual baseado no tipo de arma
        float damageBonusPercent = getDamageBonusPercent(attacker, attackDef);

        // Calcular dano final
        int damage = (int)(baseDamage * (1 + damageBonusPercent));

        // Aplicar multiplicador do ataque (ex: dano em área pode ser menor)
        damage = (int)(damage * attackDef.getDamageMultiplier());

        // Chance de crítico
        boolean wasCritical = (int)(Math.random() * 100) < (attacker.getCriticalChance() * 100);
        if (wasCritical) {
            damage = (int)(damage * attacker.getCriticalDamage());
        }

        damage = Math.max(1, damage);

        // Aplicar defesa do alvo
        damage = applyDefenseReduction(damage, target, attackDef);

        int newHp = Math.max(0, target.getCurrentHp() - damage);
        target.setCurrentHp(newHp);

        boolean targetDied = newHp <= 0;

        // Aplicar life steal (se tiver)
        applyLifeSteal(attacker, damage);

        // Aplicar knockback
        float angle = (float) Math.atan2(target.getY() - attacker.getY(), target.getX() - attacker.getX());
        float knockbackX = (float) Math.cos(angle) * attackDef.getKnockbackPower();
        float knockbackY = (float) Math.sin(angle) * attackDef.getKnockbackPower();

        if (!targetDied) {
            float newX = target.getX() + knockbackX;
            float newY = target.getY() + knockbackY;
            if (!ChunkManager.getInstance().isSolid(newX, newY)) {
                target.setX(newX);
                target.setY(newY);
            }
        }

        DatabaseManager.getInstance().savePlayerAsync(target);
        DatabaseManager.getInstance().savePlayerAsync(attacker);

        logger.info("ATTACK: {} -> {} | BaseDamage={}, Bonus={}%, FinalDamage={}{} | Target HP: {}/{}",
                attacker.getUsername(), target.getUsername(), baseDamage,
                (int)(damageBonusPercent * 100), damage,
                wasCritical ? " (CRITICAL!)" : "",
                newHp, target.getMaxHp());

        if (targetDied) {
            handlePlayerDeath(target, attacker);
        }

        return new AttackResult(true, damage, wasCritical, targetDied,
                target.getId(), target.getUsername(), newHp,
                AttackType.MELEE_SWORD, knockbackX, knockbackY);
    }

    /**
     * Obtém o dano base da arma equipada
     */
    private int getWeaponBaseDamage(Player attacker, AttackDefinition attackDef) {
        String equippedWeapon = attacker.getInventory() != null ?
                attacker.getInventory().getEquipped().get("weapon") : null;

        if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
            ItemDefinition def = ItemManager.getInstance().getItemDefinition(equippedWeapon);
            if (def != null) {
                return def.getDamage();
            }
        }

        // Dano base para soco (sem arma)
        return 5;
    }

    /**
     * Obtém o bônus percentual de dano baseado no tipo de ataque
     * @return valor entre 0.0 e 1.0 (ex: 0.20 = +20%)
     */
    private float getDamageBonusPercent(Player attacker, AttackDefinition attackDef) {
        if (attackDef.isRanged()) {
            // Armas de longo alcance (arcos)
            // RangedPower: valor base 10, cada ponto = +1%
            // Exemplo: RangedPower = 30 → +30% de dano
            return (attacker.getRangedPower() - 10) / 100f;
        } else if (attackDef.isMagic()) {
            // Magias (se implementar)
            return (attacker.getMagicPower() - 10) / 100f;
        } else {
            // Armas corpo a corpo (espadas, machados, adagas)
            // PhysicalPower: valor base 10, cada ponto = +1%
            return (attacker.getPhysicalPower() - 10) / 100f;
        }
    }

    /**
     * Aplica redução de dano baseada nas defesas do alvo
     */
    private int applyDefenseReduction(int damage, Player target, AttackDefinition attackDef) {
        float defense;

        if (!attackDef.isMagic()) {
            defense = target.getPhysicalDefense();
        } else {
            defense = target.getPhysicalDefense();
        }

        // Fórmula de redução: DR = Defense / (Defense + 100)
        // Exemplo: Defense = 50 → 50/150 = 33.3% de redução
        float damageReduction = defense / (defense + 100f);
        damageReduction = Math.min(0.75f, damageReduction); // Máximo 75% de redução

        int reducedDamage = (int)(damage * (1 - damageReduction));

        if (damageReduction > 0) {
            logger.debug("Damage reduction: {}% (defense={}) -> {} -> {}",
                    (int)(damageReduction * 100), defense, damage, reducedDamage);
        }

        return Math.max(1, reducedDamage);
    }

    /**
     * Aplica life steal baseado no dano causado
     */
    private void applyLifeSteal(Player attacker, int damage) {
        float lifeStealPercent = attacker.getLifeSteal();
        if (lifeStealPercent > 0 && damage > 0) {
            int healAmount = (int)(damage * lifeStealPercent);
            if (healAmount > 0) {
                int newHp = Math.min(attacker.getMaxHp(), attacker.getCurrentHp() + healAmount);
                attacker.setCurrentHp(newHp);
                logger.info("{} life stole {} HP ({}% of {} damage)",
                        attacker.getUsername(), healAmount, (int)(lifeStealPercent * 100), damage);
            }
        }
    }

    private void updateCombatStatsFromEquipment(Player player) {
        if (player.getCombatStats() == null) {
            player.setCombatStats(new CombatStats());
        }

        int weaponBonus = 0;
        String equippedWeapon = player.getInventory() != null ?
                player.getInventory().getEquipped().get("weapon") : null;

        if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
            ItemDefinition def = ItemManager.getInstance().getItemDefinition(equippedWeapon);
            if (def != null) {
                weaponBonus = def.getDamage();
            }
        }

        player.getCombatStats().setWeaponDamageBonus(weaponBonus);
        // Forca nao existe mais, usar PhysicalPower
        player.getCombatStats().setStrengthBonus(player.getPhysicalPower() / 2);
    }

    public Player findNearestTarget(Player attacker, AttackType attackType, Collection<Player> players) {
        float rangePixels = attackType.getRange() * 64;
        float minDistance = rangePixels;
        Player nearest = null;

        for (Player other : players) {
            if (other.getId().equals(attacker.getId())) continue;
            if (other.getCurrentHp() <= 0) continue;

            float dx = other.getX() - attacker.getX();
            float dy = other.getY() - attacker.getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            if (distance < minDistance) {
                minDistance = distance;
                nearest = other;
            }
        }

        return nearest;
    }

    public void handlePlayerDeath(Player dead, Player killer) {
        int xpGained = 50 + dead.getLevel() * 10;
        boolean leveledUp = killer.addExperience(xpGained);

        if (leveledUp) {
            logger.info("{} leveled up to level {}!", killer.getUsername(), killer.getLevel());
            ChatMessage levelUpMsg = new ChatMessage(killer.getId(), "SISTEMA",
                    killer.getUsername() + " reached level " + killer.getLevel() + "!");
            GameServerHandler.broadcastToAll(levelUpMsg);
        }

        dead.setX(400);
        dead.setY(300);
        dead.setCurrentHp(dead.getMaxHp());
        dead.setCurrentMana(dead.getMaxMana());
        dead.setCurrentStamina(dead.getMaxStamina());
        dead.setLastAttackTime(0);

        DatabaseManager.getInstance().savePlayerAsync(dead);

        ChatMessage deathMsg = new ChatMessage(dead.getId(), "SISTEMA",
                dead.getUsername() + " was killed by " + killer.getUsername() + "!");
        GameServerHandler.broadcastToAll(deathMsg);

        PlayerStatePacket deathState = new PlayerStatePacket(dead);
        deathState.fullSync = true;
        GameServerHandler.broadcastToAll(deathState);

        logger.info("Broadcast death state for {}: HP={}/{}, Pos=({},{})",
                dead.getUsername(), dead.getCurrentHp(), dead.getMaxHp(), dead.getX(), dead.getY());
    }
}