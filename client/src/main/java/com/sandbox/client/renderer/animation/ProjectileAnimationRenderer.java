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
    private static final int PROJECTILE_RENDER_SCALE = 2;

    private final Map<String, Texture> textures;
    private final Map<String, TextureRegion[]> animationFrames;
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

            TextureRegion[][] temp = TextureRegion.split(texture, anim.getFrameWidth(), anim.getFrameHeight());
            TextureRegion[] frames = temp[0];

            if (frames.length > anim.getTotalFrames()) {
                TextureRegion[] limited = new TextureRegion[anim.getTotalFrames()];
                System.arraycopy(frames, 0, limited, 0, anim.getTotalFrames());
                frames = limited;
            }

            animationFrames.put(anim.getId(), frames);

            logger.info("Loaded animation: {} | {} frames | {}x{}px | {:.2f}s/frame | Total duration: {:.2f}s",
                    anim.getId(), frames.length, anim.getFrameWidth(), anim.getFrameHeight(),
                    anim.getFrameDuration(), frames.length * anim.getFrameDuration());

        } catch (Exception e) {
            logger.error("Failed to load animation texture: {}", anim.getId(), e);
        }
    }

    public void onProjectileState(ProjectileStatePacket packet) {
        if (!packet.active) {
            activeProjectiles.remove(packet.projectileId);
            logger.debug("Projectile removed: {}", packet.projectileId);
            return;
        }

        ClientAnimatedProjectile proj = activeProjectiles.get(packet.projectileId);
        if (proj == null) {
            proj = new ClientAnimatedProjectile(packet);
            activeProjectiles.put(packet.projectileId, proj);
            logger.debug("Projectile added: {} | animation: {}", packet.projectileId, packet.animationId);
        } else {
            proj.updateFromPacket(packet);
        }
    }

    public void update(float delta) {
        for (ClientAnimatedProjectile proj : activeProjectiles.values()) {
            proj.update(delta);
        }
        activeProjectiles.values().removeIf(proj -> {
            boolean inactive = !proj.isActive();
            if (inactive) {
                logger.debug("Removing inactive projectile: {}", proj.id);
            }
            return inactive;
        });
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
        private float angle;
        private int currentFrame;
        private float animationTimer;
        private boolean isRanged;
        private boolean animationCompleted = false;  // ← NOVO CAMPO
        private boolean active = true;

        ClientAnimatedProjectile(ProjectileStatePacket packet) {
            this.id = packet.projectileId;
            this.animationId = packet.animationId;
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.angle = packet.angle;
            this.currentFrame = 0;
            this.animationTimer = 0;
            this.isRanged = "arrow".equals(packet.projectileType);
            this.animationCompleted = false;
        }

        void updateFromPacket(ProjectileStatePacket packet) {
            this.x = packet.currentX;
            this.y = packet.currentY;
            this.angle = packet.angle;
        }

        void update(float delta) {
            if (!active) return;
            if (animationCompleted) return;  // ← Se já completou, não faz nada

            ProjectileAnimation anim = animations.get(animationId);
            if (anim != null) {
                animationTimer += delta;

                if (animationTimer >= anim.getFrameDuration()) {
                    animationTimer = 0;
                    currentFrame++;

                    if (currentFrame >= anim.getTotalFrames()) {
                        if (isRanged) {
                            currentFrame = 0;      // Loop para ranged
                        } else {
                            animationCompleted = true;  // Marca como completo
                            currentFrame = anim.getTotalFrames() - 1; // Mantém no último frame
                            // OU active = false; se quiser sumir completamente
                        }
                    }
                }
            }
        }

        void render(SpriteBatch batch, GameCamera camera) {
            if (!active) return;
            if (animationCompleted && !isRanged) return;  // ← Melee completo: NÃO DESENHA

            ProjectileAnimation anim = animations.get(animationId);
            if (anim == null) return;

            TextureRegion[] frames = animationFrames.get(animationId);
            if (frames == null || frames.length == 0) return;

            // Para melee, se completou, não desenha
            if (!isRanged && animationCompleted) return;

            int frameIndex = currentFrame < frames.length ? currentFrame : frames.length - 1;
            TextureRegion frame = frames[frameIndex];

            int renderWidth = anim.getFrameWidth() * PROJECTILE_RENDER_SCALE;
            int renderHeight = anim.getFrameHeight() * PROJECTILE_RENDER_SCALE;

            float drawX = x - renderWidth / 2;
            float drawY = y - renderHeight / 2;

            batch.draw(frame, drawX, drawY, renderWidth / 2, renderHeight / 2,
                    renderWidth, renderHeight, 1f, 1f, angle);
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