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

    /**
     * Processa um ataque de um jogador contra outro
     */
    public AttackResult processAttack(Player attacker, Player target, AttackDefinition attackDef) {
        if (attacker == null || target == null) {
            return new AttackResult(false, 0, false, false, null, null, 0, AttackType.MELEE_SWORD, 0, 0);
        }

        // Atualizar stats do atacante baseado no equipamento
        updateCombatStatsFromEquipment(attacker);

        // Calcular dano
        CombatStats stats = attacker.getCombatStats();
        int damage = stats.getBaseDamage() + stats.getWeaponDamageBonus() + stats.getStrengthBonus();
        damage = (int) (damage * attackDef.getDamageMultiplier());

        // Chance de crítico
        boolean wasCritical = (int)(Math.random() * 100) < stats.getCriticalChance();
        if (wasCritical) {
            damage = damage * stats.getCriticalDamage() / 100;
        }

        damage = Math.max(1, damage);

        // Aplicar dano ao alvo
        int newHp = Math.max(0, target.getCurrentHp() - damage);
        target.setCurrentHp(newHp);

        boolean targetDied = newHp <= 0;

        // Calcular knockback (direção do atacante para o alvo)
        float angle = (float) Math.atan2(target.getY() - attacker.getY(), target.getX() - attacker.getX());
        float knockbackX = (float) Math.cos(angle) * attackDef.getKnockbackPower();
        float knockbackY = (float) Math.sin(angle) * attackDef.getKnockbackPower();

        // Aplicar knockback (apenas se não morreu)
        if (!targetDied) {
            float newX = target.getX() + knockbackX;
            float newY = target.getY() + knockbackY;

            if (!ChunkManager.getInstance().isSolid(newX, newY)) {
                target.setX(newX);
                target.setY(newY);
            }
        }

        // Salvar no banco
        DatabaseManager.getInstance().savePlayerAsync(target);
        DatabaseManager.getInstance().savePlayerAsync(attacker);

        logger.info("⚔️ ATTACK: {} -> {} | Damage: {}{} | Target HP: {}/{}",
                attacker.getUsername(), target.getUsername(), damage,
                wasCritical ? " (CRITICAL!)" : "",
                newHp, target.getMaxHp());

        if (targetDied) {
            handlePlayerDeath(target, attacker);
        }

        return new AttackResult(true, damage, wasCritical, targetDied,
                target.getId(), target.getUsername(), newHp,
                AttackType.MELEE_SWORD, knockbackX, knockbackY);
    }

    private int calculateDamageWithCritical(CombatStats stats) {
        int damage = stats.getBaseDamage() + stats.getWeaponDamageBonus() + stats.getStrengthBonus();
        damage = Math.max(1, damage);
        if (isCriticalHit(stats)) {
            damage = damage * stats.getCriticalDamage() / 100;
        }
        return damage;
    }

    private boolean isCriticalHit(CombatStats stats) {
        return (int)(Math.random() * 100) < stats.getCriticalChance();
    }

    // CombatManager.java
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
                weaponBonus = def.getDamage();  // ← USA O DANO REAL DO ITEM
                logger.debug("Weapon found: {}, Damage: {}", def.getName(), weaponBonus);
            } else {
                logger.warn("Weapon definition not found: {}", equippedWeapon);
            }
        }

        player.getCombatStats().setWeaponDamageBonus(weaponBonus);
        player.getCombatStats().setStrengthBonus(player.getStrength() / 2);
    }

    /**
     * Encontra o alvo mais próximo dentro do range de ataque
     */
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
        // Dar experiência ao killer
        int xpGained = 50 + dead.getLevel() * 10;
        boolean leveledUp = killer.addExperience(xpGained);

        if (leveledUp) {
            logger.info("🎉 {} leveled up to level {}!", killer.getUsername(), killer.getLevel());
            ChatMessage levelUpMsg = new ChatMessage(killer.getId(), "SISTEMA",
                    killer.getUsername() + " reached level " + killer.getLevel() + "!");
            GameServerHandler.broadcastToAll(levelUpMsg);
        }

        // Teleportar o morto para o spawn
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
        deathState.fullSync = true;  // Sincronização completa
        GameServerHandler.broadcastToAll(deathState);

        logger.info("📊 Broadcast death state for {}: HP={}/{}, Pos=({},{})",
                dead.getUsername(), dead.getCurrentHp(), dead.getMaxHp(), dead.getX(), dead.getY());
    }

}