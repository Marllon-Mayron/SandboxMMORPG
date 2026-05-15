package com.sandbox.server;

import com.common.sandbox.model.ItemDefinition;
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
    private static final float PROJECTILE_CHECK_INTERVAL = 1.0f / 30.0f;

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

            projectile.update(delta);

            Player hitPlayer = checkCollision(projectile, allPlayers);

            if (hitPlayer != null) {
                processHit(projectile, hitPlayer);
                removeProjectile(projectile.getId());
                continue;
            }

            if (projectile.getDistanceTraveled() >= projectile.getMaxDistance()) {
                removeProjectile(projectile.getId());
                continue;
            }

            broadcastProjectileState(projectile);
        }
    }

    private Player checkCollision(Projectile projectile, Collection<Player> players) {
        float projX = projectile.getCurrentX();
        float projY = projectile.getCurrentY();
        float collisionRadius = 32f;

        for (Player player : players) {
            if (player.getId().equals(projectile.getOwnerId())) continue;
            if (player.getCurrentHp() <= 0) continue;

            float dx = projX - player.getX();
            float dy = projY - player.getY();
            float distance = (float) Math.sqrt(dx * dx + dy * dy);

            float radius = "melee_slash".equals(projectile.getProjectileType()) ? 48f : collisionRadius;

            if (distance < radius + 24) {
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

        logger.info("PROJECTILE HIT: {} -> {} | Damage: {}{} | Target HP: {}/{} | Type: {}",
                projectile.getOwnerName(), target.getUsername(), damage,
                wasCritical ? " (CRITICAL!)" : "",
                newHp, target.getMaxHp(),
                projectile.getProjectileType());

        if (targetDied) {
            Player attacker = GameWorld.getInstance().getPlayer(projectile.getOwnerId());
            if (attacker != null) {
                CombatManager.getInstance().handlePlayerDeath(target, attacker);
            }
        }

        DatabaseManager.getInstance().savePlayerAsync(target);

        DamagePacket damagePacket = new DamagePacket(
                target.getId(), damage, wasCritical, newHp, null);

        Channel targetChannel = GameServerHandler.getChannelByPlayerId(target.getId());
        if (targetChannel != null) {
            targetChannel.writeAndFlush(damagePacket);
        }

        AttackBroadcast impact = new AttackBroadcast();
        impact.attackerId = projectile.getOwnerId();
        impact.attackerName = projectile.getOwnerName();
        impact.targetX = target.getX();
        impact.targetY = target.getY();
        GameServerHandler.broadcastToAll(impact);
    }

    public void spawnProjectile(Player attacker, String projectileType,
                                float targetX, float targetY,
                                int damage, boolean wasCritical,
                                float speed, float range, boolean isRanged) {

        // Buscar animação do item
        String animationId = "arrow"; // default

        String weaponId = attacker.getInventory() != null ?
                attacker.getInventory().getEquipped().get("weapon") : null;
        if (weaponId != null && !weaponId.isEmpty()) {
            ItemDefinition def = ItemManager.getInstance().getItemDefinition(weaponId);
            if (def != null && def.getProjectileAnimationId() != null) {
                animationId = def.getProjectileAnimationId();
            }
        }

        float angle = (float) Math.atan2(targetY - attacker.getY(), targetX - attacker.getX());
        float offsetDistance = isRanged ? 40f : 20f;
        float offsetX = (float) Math.cos(angle) * offsetDistance;
        float offsetY = (float) Math.sin(angle) * offsetDistance;
        float startX = attacker.getX() + offsetX;
        float startY = attacker.getY() + offsetY;

        float finalTargetX = targetX;
        float finalTargetY = targetY;

        if (!isRanged) {
            float dx = targetX - startX;
            float dy = targetY - startY;
            float distance = (float) Math.sqrt(dx * dx + dy * dy);
            if (distance > range && range > 0) {
                float ratio = range / distance;
                finalTargetX = startX + dx * ratio;
                finalTargetY = startY + dy * ratio;
            }
        }

        Projectile projectile = new Projectile(
                attacker.getId(), attacker.getUsername(),
                projectileType, animationId,
                startX, startY, finalTargetX, finalTargetY,
                speed, range, damage, wasCritical
        );

        activeProjectiles.put(projectile.getId(), projectile);
        broadcastProjectileState(projectile);

        logger.info("Projectile spawned: {} by {} | Speed: {} | Range: {} | Ranged: {}",
                projectileType, attacker.getUsername(), speed, range, isRanged);
    }

    private void broadcastProjectileState(Projectile projectile) {
        ProjectileStatePacket packet = new ProjectileStatePacket(projectile);
        GameServerHandler.broadcastToAll(packet);
    }

    private void removeProjectile(String id) {
        Projectile removed = activeProjectiles.remove(id);
        if (removed != null) {
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