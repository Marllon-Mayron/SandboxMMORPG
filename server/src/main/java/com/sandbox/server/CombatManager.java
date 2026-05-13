package com.sandbox.server;

import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.AttackBroadcast;
import com.common.sandbox.network.packets.ChatMessage;
import com.common.sandbox.network.packets.DamagePacket;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class CombatManager {
    private static final Logger logger = LoggerFactory.getLogger(CombatManager.class);
    private static CombatManager instance;
    private static final long GLOBAL_ATTACK_COOLDOWN_MS = 2000;

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
    public AttackResult processAttack(Player attacker, Player target, AttackType attackType) {
        if (attacker == null || target == null) {
            return new AttackResult(false, 0, false, false, null, null, 0, attackType, 0, 0);
        }

        // Verificar se pode atacar
        if (!attacker.canAttack()) {
            logger.debug("{} cannot attack yet (cooldown)", attacker.getUsername());
            return new AttackResult(false, 0, false, false, target.getId(), target.getUsername(),
                    target.getCurrentHp(), attackType, 0, 0);
        }

        // Verificar range
        float rangePixels = attackType.getRange() * 64;
        float dx = target.getX() - attacker.getX();
        float dy = target.getY() - attacker.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);

        if (distance > rangePixels) {
            logger.debug("{} target {} out of range (distance: {} > {})",
                    attacker.getUsername(), target.getUsername(), distance, rangePixels);
            return new AttackResult(false, 0, false, false, target.getId(), target.getUsername(),
                    target.getCurrentHp(), attackType, 0, 0);
        }

        // Atualizar stats do atacante baseado no equipamento
        updateCombatStatsFromEquipment(attacker);

        // Calcular dano
        CombatStats stats = attacker.getCombatStats();
        int damage = calculateDamageWithCritical(stats);
        boolean wasCritical = isCriticalHit(stats);

        // Aplicar multiplicador do tipo de ataque
        damage = (int) (damage * attackType.getDamageMultiplier());

        // Garantir dano mínimo
        damage = Math.max(1, damage);

        // Aplicar dano ao alvo
        int newHp = Math.max(0, target.getCurrentHp() - damage);
        target.setCurrentHp(newHp);

        boolean targetDied = newHp <= 0;

        // Calcular knockback
        float angle = (float) Math.atan2(target.getY() - attacker.getY(), target.getX() - attacker.getX());
        float knockbackX = (float) Math.cos(angle) * 30;
        float knockbackY = (float) Math.sin(angle) * 30;

        // Aplicar knockback (apenas se não morreu)
        if (!targetDied) {
            float newX = target.getX() + knockbackX;
            float newY = target.getY() + knockbackY;

            if (!ChunkManager.getInstance().isSolid(newX, newY)) {
                target.setX(newX);
                target.setY(newY);
            }
        }

        // Executar o ataque no attacker (iniciar cooldown)
        attacker.executeAttack();

        // Salvar no banco (assíncrono)
        DatabaseManager.getInstance().savePlayerPositionAsync(target);

        logger.info("⚔️ ATTACK: {} -> {} | Damage: {}{} | Target HP: {}/{} | Range: {:.1f}",
                attacker.getUsername(), target.getUsername(), damage,
                wasCritical ? " (CRITICAL!)" : "",
                newHp, target.getMaxHp(), distance);

        if (targetDied) {
            logger.info("💀 {} killed {}!", attacker.getUsername(), target.getUsername());
            handlePlayerDeath(target, attacker);
        }

        return new AttackResult(true, damage, wasCritical, targetDied,
                target.getId(), target.getUsername(), newHp,
                attackType, knockbackX, knockbackY);
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

    private void updateCombatStatsFromEquipment(Player player) {
        if (player.getCombatStats() == null) {
            player.setCombatStats(new CombatStats());
        }

        int weaponBonus = 0;
        String equippedWeapon = player.getInventory() != null ?
                player.getInventory().getEquipped().get("weapon") : null;

        if (equippedWeapon != null && !equippedWeapon.isEmpty()) {
            if (equippedWeapon.contains("sword")) weaponBonus = 5;
            else if (equippedWeapon.contains("dagger")) weaponBonus = 3;
            else if (equippedWeapon.contains("axe")) weaponBonus = 8;
            else if (equippedWeapon.contains("hammer")) weaponBonus = 10;
            else if (equippedWeapon.contains("bow")) weaponBonus = 4;
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

    private void handlePlayerDeath(Player dead, Player killer) {
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

        DatabaseManager.getInstance().savePlayerPositionAsync(dead);

        ChatMessage deathMsg = new ChatMessage(dead.getId(), "SISTEMA",
                dead.getUsername() + " was killed by " + killer.getUsername() + "!");
        GameServerHandler.broadcastToAll(deathMsg);

        Player movementOnly = new Player();
        movementOnly.setId(dead.getId());
        movementOnly.setUsername(dead.getUsername());
        movementOnly.setX(dead.getX());
        movementOnly.setY(dead.getY());
        movementOnly.setDirection(dead.getDirection());
        GameServerHandler.broadcastToAll(new com.common.sandbox.network.packets.MovementBroadcast(movementOnly));
    }
}