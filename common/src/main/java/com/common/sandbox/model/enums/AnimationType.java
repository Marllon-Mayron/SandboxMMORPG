// AnimationType.java
package com.common.sandbox.model.enums;

public enum AnimationType {
    ARROW("arrow", "animations/combat/projectiles/arrow.png", 32, 32, 2, 0.08f, true, false),
    SLASH("slash", "animations/combat/projectiles/slash.png", 32, 32, 3, 0.02f, false, true),
    FIREBALL("fireball", "animations/combat/projectiles/fireball.png", 32, 32, 4, 0.05f, true, false),
    ICE_BOLT("ice_bolt", "animations/combat/projectiles/ice_bolt.png", 32, 32, 4, 0.05f, true, false),
    LIGHTNING("lightning", "animations/combat/projectiles/lightning.png", 48, 48, 5, 0.04f, true, false),
    MAGIC_BOLT("magic_bolt", "animations/combat/projectiles/magic_bolt.png", 32, 32, 4, 0.05f, true, false),
    SWORD_SLASH("sword_slash", "animations/combat/melee/sword_slash.png", 64, 64, 5, 0.03f, false, true),
    AXE_SWING("axe_swing", "animations/combat/melee/axe_swing.png", 64, 64, 6, 0.04f, false, true),
    DAGGER_STAB("dagger_stab", "animations/combat/melee/dagger_stab.png", 48, 48, 3, 0.02f, false, true),
    CAST("cast", "animations/combat/magic/cast.png", 48, 48, 4, 0.05f, false, true);

    private final String id;
    private final String spritesheetPath;
    private final int frameWidth;
    private final int frameHeight;
    private final int totalFrames;
    private final float frameDuration;
    private final boolean isRanged;
    private final boolean isMelee;

    AnimationType(String id, String spritesheetPath, int frameWidth, int frameHeight,
                  int totalFrames, float frameDuration, boolean isRanged, boolean isMelee) {
        this.id = id;
        this.spritesheetPath = spritesheetPath;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.totalFrames = totalFrames;
        this.frameDuration = frameDuration;
        this.isRanged = isRanged;
        this.isMelee = isMelee;
    }

    public String getId() { return id; }
    public String getSpritesheetPath() { return spritesheetPath; }
    public int getFrameWidth() { return frameWidth; }
    public int getFrameHeight() { return frameHeight; }
    public int getTotalFrames() { return totalFrames; }
    public float getFrameDuration() { return frameDuration; }
    public boolean isRanged() { return isRanged; }
    public boolean isMelee() { return isMelee; }

    public static AnimationType fromId(String id) {
        if (id == null) return null;
        for (AnimationType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        return null;
    }

    public static AnimationType getDefaultForRanged() {
        return ARROW;
    }

    public static AnimationType getDefaultForMelee() {
        return SLASH;
    }

    public boolean isValidForRanged() {
        return isRanged;
    }

    public boolean isValidForMelee() {
        return isMelee;
    }
}