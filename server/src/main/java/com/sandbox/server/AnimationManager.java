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
        // Arrow - 8 direções, 2 frames cada
        ProjectileAnimation arrowAnim = new ProjectileAnimation(
                "arrow",
                "animations/combat/projectiles/arrow.png",
                32, 32,   // largura x altura
                2,        // 2 frames por direção
                0.1f      // 0.1 segundos por frame
        );
        projectileAnimations.put("arrow", arrowAnim);
        logger.info("Loaded projectile animation: arrow");

        // TODO: Adicionar outras animações quando tiver os sprites
        // Exemplo para fireball:
        // ProjectileAnimation fireballAnim = new ProjectileAnimation(
        //     "fireball", "animations/combat/projectiles/fireball.png",
        //     64, 64, 3, 0.1f
        // );
        // projectileAnimations.put("fireball", fireballAnim);
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