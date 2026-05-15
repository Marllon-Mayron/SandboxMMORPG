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
                32, 32,   // largura x altura do frame
                4,        // 4 frames na animação
                0.1f      // 0.1 segundos por frame
        );
        projectileAnimations.put("arrow", arrowAnim);
        logger.info("Loaded projectile animation: arrow ({} frames)", arrowAnim.getTotalFrames());

        // Animações para ataques corpo a corpo (melee)
        // Slash - para espada e machado
        ProjectileAnimation slashAnim = new ProjectileAnimation(
                "slash",
                "animations/combat/projectiles/slash.png",
                32, 32,
                3,
                0.01f
        );
        projectileAnimations.put("slash", slashAnim);
        logger.info("Loaded melee animation: slash ({} frames)", slashAnim.getTotalFrames());


        ProjectileAnimation stabAnim = new ProjectileAnimation(
                "stab",
                "animations/combat/projectiles/slash.png", // usa mesma imagem
                32, 32, 3, 0.07f
        );
        projectileAnimations.put("stab", stabAnim);
        logger.info("Loaded melee animation: stab ({} frames)", stabAnim.getTotalFrames());

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