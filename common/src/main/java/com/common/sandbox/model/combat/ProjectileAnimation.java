package com.common.sandbox.model.combat;

import java.io.Serializable;

public class ProjectileAnimation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String spritesheetPath;
    private int frameWidth;
    private int frameHeight;
    private int totalFrames;
    private float frameDuration;
    private int totalDirections;

    public ProjectileAnimation() {}

    public ProjectileAnimation(String id, String spritesheetPath,
                               int frameWidth, int frameHeight,
                               int totalFrames, float frameDuration) {
        this.id = id;
        this.spritesheetPath = spritesheetPath;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.totalFrames = totalFrames;
        this.frameDuration = frameDuration;
        this.totalDirections = 1;
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSpritesheetPath() { return spritesheetPath; }
    public void setSpritesheetPath(String spritesheetPath) { this.spritesheetPath = spritesheetPath; }

    public int getFrameWidth() { return frameWidth; }
    public void setFrameWidth(int frameWidth) { this.frameWidth = frameWidth; }

    public int getFrameHeight() { return frameHeight; }
    public void setFrameHeight(int frameHeight) { this.frameHeight = frameHeight; }

    public int getTotalFrames() { return totalFrames; }
    public void setTotalFrames(int totalFrames) { this.totalFrames = totalFrames; }

    public float getFrameDuration() { return frameDuration; }
    public void setFrameDuration(float frameDuration) { this.frameDuration = frameDuration; }

    public int getTotalDirections() { return totalDirections; }
    public void setTotalDirections(int totalDirections) { this.totalDirections = totalDirections; }
}