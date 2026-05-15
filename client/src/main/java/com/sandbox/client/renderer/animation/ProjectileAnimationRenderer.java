package com.sandbox.client.renderer.animation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.common.sandbox.model.ProjectileAnimation;
import com.common.sandbox.network.packets.AnimationSyncPacket;
import com.common.sandbox.network.packets.ProjectileStatePacket;
import com.sandbox.client.camera.GameCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectileAnimationRenderer {
    private static final Logger logger = LoggerFactory.getLogger(ProjectileAnimationRenderer.class);

    private final Map<String, Texture> textures;
    private final Map<String, TextureRegion[][]> animationFrames;
    private final Map<String, ProjectileAnimation> animations;
    private final Map<String, ClientAnimatedProjectile> activeProjectiles;

    public ProjectileAnimationRenderer() {
        this.textures = new ConcurrentHashMap<>();
        this.animationFrames = new ConcurrentHashMap<>();
        this.animations = new ConcurrentHashMap<>();
        this.activeProjectiles = new ConcurrentHashMap<>();
    }

    public void onAnimationSync(AnimationSyncPacket packet) {
        if (packet.projectileAnimations == null) return;

        logger.info("Received {} projectile animations from server", packet.projectileAnimations.size());

        for (Map.Entry<String, ProjectileAnimation> entry : packet.projectileAnimations.entrySet()) {
            String id = entry.getKey();
            ProjectileAnimation anim = entry.getValue();
            loadAnimationTexture(anim);
        }
    }

    private void loadAnimationTexture(ProjectileAnimation anim) {
        String path = anim.getSpritesheetPath();

        try {
            String[] possiblePaths = {
                    path,
                    "assets/" + path,
                    "../client/assets/" + path,
                    "C:/Users/Marllon/IdeaProjects/sandbox-simulator/client/assets/" + path
            };

            FileHandle file = null;
            for (String tryPath : possiblePaths) {
                FileHandle f = Gdx.files.internal(tryPath);
                if (f.exists()) {
                    file = f;
                    logger.info("Found animation sprite at: {}", tryPath);
                    break;
                }
            }

            if (file == null) {
                logger.error("Animation sprite not found for: {} at any path", anim.getId());
                return;
            }

            Texture texture = new Texture(file);
            texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            textures.put(anim.getId(), texture);
            animations.put(anim.getId(), anim);

            int cols = texture.getWidth() / anim.getFrameWidth();
            int rows = texture.getHeight() / anim.getFrameHeight();
            TextureRegion[][] frames = TextureRegion.split(texture, anim.getFrameWidth(), anim.getFrameHeight());
            animationFrames.put(anim.getId(), frames);

            logger.info("Loaded animation: {} | {}x{} frames | {}x{}px | {} frames/dir | {:.2f}s/frame",
                    anim.getId(), cols, rows, anim.getFrameWidth(), anim.getFrameHeight(),
                    anim.getFramesPerDirection(), anim.getFrameDuration());

        } catch (Exception e) {
            logger.error("Failed to load animation texture: {}", anim.getId(), e);
        }
    }

    public void onProjectileState(ProjectileStatePacket packet) {
        if (!packet.active) {
            activeProjectiles.remove(packet.projectileId);
            return;
        }

        ClientAnimatedProjectile proj = activeProjectiles.get(packet.projectileId);
        if (proj == null) {
            proj = new ClientAnimatedProjectile(packet);
            activeProjectiles.put(packet.projectileId, proj);
        } else {
            proj.updateFromPacket(packet);
        }
    }

    public void update(float delta) {
        for (ClientAnimatedProjectile proj : activeProjectiles.values()) {
            proj.update(delta);
        }
        activeProjectiles.values().removeIf(proj -> !proj.isActive());
    }

    public void render(SpriteBatch batch, GameCamera camera) {
        for (ClientAnimatedProjectile proj : activeProjectiles.values()) {
            proj.render(batch, camera);
        }
    }

    private class ClientAnimatedProjectile {
        private String id;
        private String animationId;
        private float x, y;
        private int direction;  // ← CAMPO ADICIONADO
        private float currentFrame;
        private float animationTimer;
        private boolean active = true;

        ClientAnimatedProjectile(ProjectileStatePacket packet) {
            this.id = packet.projectileId;
            this.animationId = packet.animationId;
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.direction = packet.direction;  // ← CORRIGIDO
            this.currentFrame = 0;
            this.animationTimer = 0;

            // Log para debug
            System.out.println("Projectile created: " + id + " direction=" + direction);
        }

        void updateFromPacket(ProjectileStatePacket packet) {
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.direction = packet.direction;  // ← CORRIGIDO
        }

        void update(float delta) {
            if (!active) return;

            ProjectileAnimation anim = animations.get(animationId);
            if (anim != null) {
                animationTimer += delta;
                if (animationTimer >= anim.getFrameDuration()) {
                    animationTimer = 0;
                    currentFrame++;
                    if (currentFrame >= anim.getFramesPerDirection()) {
                        currentFrame = 0;
                    }
                }
            }
        }

        void render(SpriteBatch batch, GameCamera camera) {
            ProjectileAnimation anim = animations.get(animationId);
            if (anim == null) return;

            TextureRegion[][] frames = animationFrames.get(animationId);
            if (frames == null) return;

            // Usar a direção que veio do servidor
            int row = direction;
            int col = (int) currentFrame;

            if (row < frames.length && col < frames[0].length) {
                TextureRegion frame = frames[row][col];
                float drawX = x - anim.getFrameWidth() / 2;
                float drawY = y - anim.getFrameHeight() / 2;
                batch.draw(frame, drawX, drawY, anim.getFrameWidth(), anim.getFrameHeight());
            } else {
                // Log se a direção for inválida
                if (row >= frames.length) {
                    System.out.println("Invalid direction: row=" + row + " max=" + frames.length);
                }
            }
        }

        boolean isActive() { return active; }
    }

    public void dispose() {
        for (Texture t : textures.values()) {
            t.dispose();
        }
        textures.clear();
        animationFrames.clear();
        animations.clear();
        activeProjectiles.clear();
    }
}