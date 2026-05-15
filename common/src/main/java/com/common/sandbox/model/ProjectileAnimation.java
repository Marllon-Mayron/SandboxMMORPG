package com.common.sandbox.model;

import java.io.Serializable;

public class ProjectileAnimation implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String spritesheetPath;
    private int frameWidth;
    private int frameHeight;
    private int framesPerDirection;
    private float frameDuration;
    private int totalDirections; // 8 para 8 direções

    public ProjectileAnimation() {}

    public ProjectileAnimation(String id, String spritesheetPath,
                               int frameWidth, int frameHeight,
                               int framesPerDirection, float frameDuration) {
        this.id = id;
        this.spritesheetPath = spritesheetPath;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.framesPerDirection = framesPerDirection;
        this.frameDuration = frameDuration;
        this.totalDirections = 8;
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

    public int getFramesPerDirection() { return framesPerDirection; }
    public void setFramesPerDirection(int framesPerDirection) { this.framesPerDirection = framesPerDirection; }

    public float getFrameDuration() { return frameDuration; }
    public void setFrameDuration(float frameDuration) { this.frameDuration = frameDuration; }

    public int getTotalDirections() { return totalDirections; }
    public void setTotalDirections(int totalDirections) { this.totalDirections = totalDirections; }
}