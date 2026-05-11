package com.sandbox.client.editor.models;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;

public class SpritesheetData {
    private String name;
    private String path;  // ✅ Path relativo aos assets (ex: "world/outside.png")
    private Texture texture;
    private TextureRegion[][] sprites;
    private int rows;
    private int cols;
    private boolean isDefault;

    public SpritesheetData(String name, String path, Texture texture) {
        this(name, path, texture, false);
    }

    public SpritesheetData(String name, String path, Texture texture, boolean isDefault) {
        this.name = name;
        this.path = path;
        this.texture = texture;
        this.isDefault = isDefault;
        this.cols = texture.getWidth() / 32;
        this.rows = texture.getHeight() / 32;
        this.sprites = TextureRegion.split(texture, 32, 32);
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public Texture getTexture() { return texture; }
    public TextureRegion[][] getSprites() { return sprites; }
    public int getRows() { return rows; }
    public int getCols() { return cols; }
    public boolean isDefault() { return isDefault; }

    public int getTotalSprites() { return rows * cols; }

    public TextureRegion getSprite(int index) {
        if (index < 0 || index >= getTotalSprites()) return null;
        return sprites[index / cols][index % cols];
    }

    public void dispose() {
        if (texture != null && !isDefault) {
            texture.dispose();
        }
    }
}