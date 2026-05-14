package com.sandbox.server;

import com.common.sandbox.model.AttackDefinition;
import com.common.sandbox.model.Player;
import com.common.sandbox.model.Projectile;
import com.common.sandbox.network.packets.AttackBroadcast;
import com.common.sandbox.network.packets.DamagePacket;
import com.common.sandbox.network.packets.ProjectileStatePacket;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ProjectileManager {
    private static final Logger logger = LoggerFactory.getLogger(ProjectileManager.class);
    private static ProjectileManager instance;

    private final Map<String, Projectile> activeProjectiles;
    private final ScheduledExecutorService scheduler;
    private static final float PROJECTILE_CHECK_INTERVAL = 1.0f / 30.0f; // 30 FPS para projéteis

    private ProjectileManager() {
        this.activeProjectiles = new ConcurrentHashMap<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        startProjectileUpdateTask();
    }

    public static synchronized ProjectileManager getInstance() {
        if (instance == null) {
            instance = new ProjectileManager();
        }
        return instance;
    }

    private void startProjectileUpdateTask() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                updateAllProjectiles();
            } catch (Exception e) {
                logger.error("Error updating projectiles", e);
            }
        }, 0, (long)(PROJECTILE_CHECK_INTERVAL * 1000), TimeUnit.MILLISECONDS);
    }

    private void updateAllProjectiles() {
        Collection<Player> allPlayers = GameWorld.getInstance().getAllPlayers();
        float delta = PROJECTILE_CHECK_INTERVAL;

        for (Projectile projectile : activeProjectiles.values()) {
            if (!projectile.isActive()) {
                removeProjectile(projectile.getId());
                continue;
            }

            // Atualizar posição
            projectile.update(delta);

            // Verificar colisão com jogadores
            Player hitPlayer = checkCollision(projectile, allPlayers);

            if (hitPlayer != null) {
                // Acertou alguém!
                processHit(projectile, hitPlayer);
                removeProjectile(projectile.getId());
                continue;
            }

            // Verificar se atingiu distância máxima
            if (projectile.getDistanceTraveled() >= projectile.getMaxDistance()) {
                removeProjectile(projectile.getId());
                continue;
            }

            // Broadcast da posição atualizada para todos
            broadcastProjectileState(projectile);
        }
    }

    private Player checkCollision(Projectile projectile, Collection<Player> players) {
        float projX = projectile.getCurrentX();
        float projY = projectile.getCurrentY();
        float collisionRadius = 32f; // Raio de colisão do projétil

        for (Player player : players) {
            if (player.getId().equals(projectile.getOwnerId())) continue; // Não acerta o atirador
            if (player.getCurrentHp() <= 0) continue;

            float dx = projX - player.getX();
            float dy = projY - player.getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            // Raio do jogador é 24 (metade de PLAYER_SIZE/2)
            if (distance < collisionRadius + 24) {
                return player;
            }
        }
        return null;
    }

    private void processHit(Projectile projectile, Player target) {
        int damage = (int) projectile.getDamage();
        boolean wasCritical = projectile.isWasCritical();

        int newHp = Math.max(0, target.getCurrentHp() - damage);
        target.setCurrentHp(newHp);

        boolean targetDied = newHp <= 0;

        logger.info("🏹 PROJECTILE HIT: {} -> {} | Damage: {}{} | Target HP: {}/{}",
                projectile.getOwnerName(), target.getUsername(), damage,
                wasCritical ? " (CRITICAL!)" : "",
                newHp, target.getMaxHp());

        if (targetDied) {
            CombatManager.getInstance().handlePlayerDeath(target,
                    GameWorld.getInstance().getPlayer(projectile.getOwnerId()));
        }

        // Salvar estado do alvo
        DatabaseManager.getInstance().savePlayerAsync(target);

        // Enviar pacote de dano para o alvo
        DamagePacket damagePacket = new DamagePacket(
                target.getId(), damage, wasCritical, newHp, null);

        Channel targetChannel = GameServerHandler.getChannelByPlayerId(target.getId());
        if (targetChannel != null) {
            targetChannel.writeAndFlush(damagePacket);
        }

        // Broadcast do impacto
        AttackBroadcast impact = new AttackBroadcast();
        impact.attackerId = projectile.getOwnerId();
        impact.attackerName = projectile.getOwnerName();
        impact.targetX = target.getX();
        impact.targetY = target.getY();
        GameServerHandler.broadcastToAll(impact);
    }

    public void spawnProjectile(Player attacker, AttackDefinition attackDef,
                                float targetX, float targetY,
                                int damage, boolean wasCritical) {
        float projectileSpeed = attackDef.getProjectileSpeed();
        if (projectileSpeed <= 0) projectileSpeed = 600f;

        float maxDistance = attackDef.getRange();
        if (maxDistance <= 0) maxDistance = 400f;

        // Calcular offset para o projétil não nascer dentro do jogador
        float angle = (float) Math.atan2(targetY - attacker.getY(), targetX - attacker.getX());
        float offsetX = (float) Math.cos(angle) * 40;
        float offsetY = (float) Math.sin(angle) * 40;
        float startX = attacker.getX() + offsetX;
        float startY = attacker.getY() + offsetY;

        Projectile projectile = new Projectile(
                attacker.getId(), attacker.getUsername(),
                attackDef.getProjectileId() != null ? attackDef.getProjectileId() : "arrow",
                startX, startY, targetX, targetY,
                projectileSpeed, maxDistance, damage, wasCritical
        );

        activeProjectiles.put(projectile.getId(), projectile);

        // Broadcast do novo projétil para todos
        broadcastProjectileState(projectile);

        logger.info("🎯 Projectile spawned: {} by {} | Target: ({}, {}) | Max distance: {}",
                projectile.getProjectileType(), attacker.getUsername(), targetX, targetY, maxDistance);
    }

    private void broadcastProjectileState(Projectile projectile) {
        ProjectileStatePacket packet = new ProjectileStatePacket(projectile);
        GameServerHandler.broadcastToAll(packet);
    }

    private void removeProjectile(String id) {
        Projectile removed = activeProjectiles.remove(id);
        if (removed != null) {
            // Broadcast que o projétil sumiu
            ProjectileStatePacket despawnPacket = new ProjectileStatePacket(removed);
            despawnPacket.active = false;
            GameServerHandler.broadcastToAll(despawnPacket);
        }
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        activeProjectiles.clear();
    }
}