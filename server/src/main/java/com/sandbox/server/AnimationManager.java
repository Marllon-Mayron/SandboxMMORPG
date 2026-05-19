// AnimationManager.java (versão atualizada)
package com.sandbox.server;

import com.common.sandbox.model.combat.ProjectileAnimation;
import com.common.sandbox.model.enums.AnimationType;
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
        // Carregar todas as animações do enum
        for (AnimationType animType : AnimationType.values()) {
            ProjectileAnimation anim = new ProjectileAnimation(
                    animType.getId(),
                    animType.getSpritesheetPath(),
                    animType.getFrameWidth(),
                    animType.getFrameHeight(),
                    animType.getTotalFrames(),
                    animType.getFrameDuration()
            );
            projectileAnimations.put(animType.getId(), anim);
            logger.debug("Loaded animation: {} (ranged: {}, melee: {})",
                    animType.getId(), animType.isRanged(), animType.isMelee());
        }

        // Verificar se as animações padrão existem
        validateDefaultAnimations();
    }

    private void validateDefaultAnimations() {
        AnimationType defaultRanged = AnimationType.getDefaultForRanged();
        AnimationType defaultMelee = AnimationType.getDefaultForMelee();

        if (!projectileAnimations.containsKey(defaultRanged.getId())) {
            logger.error("DEFAULT RANGED ANIMATION '{}' NOT FOUND! Create it in AnimationType enum.",
                    defaultRanged.getId());
        } else {
            logger.info("Default ranged animation: {} ✓", defaultRanged.getId());
        }

        if (!projectileAnimations.containsKey(defaultMelee.getId())) {
            logger.error("DEFAULT MELEE ANIMATION '{}' NOT FOUND! Create it in AnimationType enum.",
                    defaultMelee.getId());
        } else {
            logger.info("Default melee animation: {} ✓", defaultMelee.getId());
        }
    }

    public ProjectileAnimation getAnimation(String id) {
        return getAnimation(id, false);
    }

    public ProjectileAnimation getAnimation(String id, boolean isRanged) {
        ProjectileAnimation anim = projectileAnimations.get(id);

        if (anim == null) {
            // Tentar encontrar por tipo no enum primeiro
            AnimationType animType = AnimationType.fromId(id);
            if (animType != null) {
                logger.warn("Animation '{}' exists in enum but not loaded, loading now...", id);
                anim = new ProjectileAnimation(
                        animType.getId(),
                        animType.getSpritesheetPath(),
                        animType.getFrameWidth(),
                        animType.getFrameHeight(),
                        animType.getTotalFrames(),
                        animType.getFrameDuration()
                );
                projectileAnimations.put(id, anim);
                return anim;
            }

            // Usar animação padrão baseada no tipo
            AnimationType defaultType = isRanged ? AnimationType.getDefaultForRanged() : AnimationType.getDefaultForMelee();
            logger.warn("Animation '{}' not found! Using default {} animation: '{}'",
                    id, isRanged ? "ranged" : "melee", defaultType.getId());
            return projectileAnimations.get(defaultType.getId());
        }

        // Validar se a animação é compatível com o tipo de ataque
        AnimationType animType = AnimationType.fromId(anim.getId());
        if (animType != null) {
            if (isRanged && !animType.isRanged()) {
                logger.warn("Animation '{}' is not a ranged animation! Using default ranged: '{}'",
                        id, AnimationType.getDefaultForRanged().getId());
                return projectileAnimations.get(AnimationType.getDefaultForRanged().getId());
            }
            if (!isRanged && !animType.isMelee() && !animType.getId().equals("slash")) {
                // Algumas animações podem ser usadas para melee mesmo não marcadas como melee
                // Ex: cast pode ser usado para magia melee? Por segurança, logamos warning
                logger.debug("Animation '{}' used for melee attack (may not be optimized)", id);
            }
        }

        return anim;
    }

    public boolean isAnimationValidForType(String animationId, boolean isRanged) {
        AnimationType animType = AnimationType.fromId(animationId);
        if (animType == null) return false;
        return isRanged ? animType.isRanged() : animType.isMelee();
    }

    public ProjectileAnimation getDefaultRangedAnimation() {
        return projectileAnimations.get(AnimationType.getDefaultForRanged().getId());
    }

    public ProjectileAnimation getDefaultMeleeAnimation() {
        return projectileAnimations.get(AnimationType.getDefaultForMelee().getId());
    }

    public Map<String, ProjectileAnimation> getAllProjectileAnimations() {
        return new HashMap<>(projectileAnimations);
    }
}