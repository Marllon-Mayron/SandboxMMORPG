package com.sandbox.server;

import com.common.sandbox.model.ProjectileAnimation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationManager {
    private static final Logger logger = LoggerFactory.getLogger(AnimationManager.class);
    private static AnimationManager instance;

    private final Map<String, ProjectileAnimation> projectileAnimations;

    private AnimationManager() {
        this.projectileAnimations = new ConcurrentHashMap<>();
        loadProjectileAnimations();
    }

    public static synchronized AnimationManager getInstance() {
        if (instance == null) {
            instance = new AnimationManager();
        }
        return instance;
    }

    private void loadProjectileAnimations() {
        // Animação da flecha (projétil)
        ProjectileAnimation arrowAnim = new ProjectileAnimation(
                "arrow",
                "animations/combat/projectiles/arrow.png",
                32, 32,
                2,
                0.08f
        );
        projectileAnimations.put("arrow", arrowAnim);
        logger.info("Loaded projectile animation: arrow ({} frames, {} s/frame)",
                arrowAnim.getTotalFrames(), arrowAnim.getFrameDuration());

        // Animações para ataques corpo a corpo (melee)
        ProjectileAnimation slashAnim = new ProjectileAnimation(
                "slash",
                "animations/combat/projectiles/slash.png",
                32, 32,
                3,
                0.02f
        );
        projectileAnimations.put("slash", slashAnim);
        logger.info("Loaded melee animation: slash ({} frames, {} s/frame, total {} s)",
                slashAnim.getTotalFrames(),
                slashAnim.getFrameDuration(),
                slashAnim.getTotalFrames() * slashAnim.getFrameDuration());

        ProjectileAnimation stabAnim = new ProjectileAnimation(
                "stab",
                "animations/combat/projectiles/slash.png",
                32, 32,
                2,        // 2 frames
                0.25f     // 0.25 segundos por frame
        );
    }

    public ProjectileAnimation getAnimation(String id) {
        ProjectileAnimation anim = projectileAnimations.get(id);
        if (anim == null) {
            logger.warn("Animation not found: {}, using default arrow", id);
            return projectileAnimations.get("arrow");
        }
        return anim;
    }

    public Map<String, ProjectileAnimation> getAllProjectileAnimations() {
        return new HashMap<>(projectileAnimations);
    }
}