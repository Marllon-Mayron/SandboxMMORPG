package com.sandbox.client.renderer.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.common.sandbox.network.packets.ProjectileStatePacket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileRenderer {
    private final Map<String, ClientProjectile> projectiles = new ConcurrentHashMap<>();

    public void onProjectileState(ProjectileStatePacket packet) {
        if (!packet.active) {
            projectiles.remove(packet.projectileId);
            return;
        }

        ClientProjectile proj = projectiles.get(packet.projectileId);
        if (proj == null) {
            proj = new ClientProjectile(packet);
            projectiles.put(packet.projectileId, proj);
        } else {
            proj.updateFromPacket(packet);
        }
    }

    public void update(float delta) {
        projectiles.values().removeIf(proj -> {
            proj.update(delta);
            return !proj.isActive();
        });
    }

    public void renderFilled(ShapeRenderer shapeRenderer) {
        for (ClientProjectile proj : projectiles.values()) {
            proj.render(shapeRenderer);
        }
    }

    private static class ClientProjectile {
        private String id;
        private String type;
        private float x, y;
        private float directionX, directionY;
        private float progress;
        private float startX, startY;
        private float targetX, targetY;
        private float maxDistance;
        private float distanceTraveled;
        private boolean active = true;

        ClientProjectile(ProjectileStatePacket packet) {
            this.id = packet.projectileId;
            this.type = packet.projectileType;
            this.startX = packet.startX;
            this.startY = packet.startY;
            this.targetX = packet.targetX;
            this.targetY = packet.targetY;
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.directionX = packet.directionX;
            this.directionY = packet.directionY;
            this.maxDistance = packet.maxDistance;
            this.distanceTraveled = packet.distanceTraveled;

            float totalDistance = (float) Math.hypot(targetX - startX, targetY - startY);
            this.progress = totalDistance > 0 ? distanceTraveled / totalDistance : 0;
        }

        void updateFromPacket(ProjectileStatePacket packet) {
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.distanceTraveled = packet.distanceTraveled;

            float totalDistance = (float) Math.hypot(targetX - startX, targetY - startY);
            this.progress = totalDistance > 0 ? distanceTraveled / totalDistance : 0;
        }

        void update(float delta) {
            if (!active) return;
            // O cliente apenas renderiza - a posição vem do servidor
        }

        void render(ShapeRenderer shapeRenderer) {
            if (!active) return;

            float alpha = 1.0f - Math.min(1.0f, progress * 0.8f);

            switch (type) {
                case "arrow":
                    renderArrow(shapeRenderer, alpha);
                    break;
                case "fireball":
                    renderFireball(shapeRenderer, alpha);
                    break;
                case "bullet":
                    renderBullet(shapeRenderer, alpha);
                    break;
                case "melee_slash":
                    renderMeleeSlash(shapeRenderer, alpha);
                    break;
                default:
                    renderDefault(shapeRenderer, alpha);
            }
        }

        private void renderArrow(ShapeRenderer shapeRenderer, float alpha) {
            float angle = (float) Math.atan2(directionY, directionX);
            float tipX = x + (float) Math.cos(angle) * 12;
            float tipY = y + (float) Math.sin(angle) * 12;

            shapeRenderer.setColor(0.7f, 0.5f, 0.2f, alpha);
            shapeRenderer.line(x, y, tipX, tipY);

            float perpX = (float) Math.cos(angle + Math.PI/2) * 5;
            float perpY = (float) Math.sin(angle + Math.PI/2) * 5;

            shapeRenderer.setColor(0.9f, 0.7f, 0.3f, alpha);
            shapeRenderer.triangle(
                    tipX, tipY,
                    tipX - 10 * (float) Math.cos(angle) + perpX,
                    tipY - 10 * (float) Math.sin(angle) + perpY,
                    tipX - 10 * (float) Math.cos(angle) - perpX,
                    tipY - 10 * (float) Math.sin(angle) - perpY
            );
        }

        private void renderFireball(ShapeRenderer shapeRenderer, float alpha) {
            float size = 12;
            shapeRenderer.setColor(1f, 0.5f, 0.1f, alpha);
            shapeRenderer.circle(x, y, size);
            shapeRenderer.setColor(1f, 0.9f, 0.2f, alpha * 0.8f);
            shapeRenderer.circle(x, y, size - 3);

            shapeRenderer.setColor(1f, 0.4f, 0.1f, alpha * 0.5f);
            shapeRenderer.circle(x - directionX * 8, y - directionY * 8, size - 4);
        }

        private void renderBullet(ShapeRenderer shapeRenderer, float alpha) {
            shapeRenderer.setColor(0.9f, 0.9f, 0.2f, alpha);
            shapeRenderer.rect(x - 3, y - 2, 6, 4);
        }

        private void renderMeleeSlash(ShapeRenderer shapeRenderer, float alpha) {
            float angle = (float) Math.atan2(directionY, directionX);
            float length = 40 * (1 - progress);

            shapeRenderer.setColor(0.9f, 0.7f, 0.3f, alpha);

            // Desenhar um arco de corte
            for (int i = 0; i <= 8; i++) {
                float t = i / 8f;
                float offset = (float) Math.sin(t * Math.PI) * 15 * (1 - progress);
                float perpX = (float) Math.cos(angle + Math.PI/2) * offset;
                float perpY = (float) Math.sin(angle + Math.PI/2) * offset;

                float arcX = x + (float) Math.cos(angle) * (t * length) + perpX;
                float arcY = y + (float) Math.sin(angle) * (t * length) + perpY;

                shapeRenderer.circle(arcX, arcY, 4);
            }

            // Ponto principal
            shapeRenderer.setColor(1f, 0.9f, 0.5f, alpha);
            shapeRenderer.circle(x, y, 8);
        }

        private void renderDefault(ShapeRenderer shapeRenderer, float alpha) {
            shapeRenderer.setColor(1f, 1f, 1f, alpha);
            shapeRenderer.circle(x, y, 6);
        }

        boolean isActive() { return active; }
    }
}