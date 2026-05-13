package com.sandbox.client.renderer;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.common.sandbox.model.GroundItem;
import com.sandbox.client.camera.GameCamera;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ItemRenderer {
    private static final Logger logger = LoggerFactory.getLogger(ItemRenderer.class);

    private final Map<String, Texture> spritesheets = new ConcurrentHashMap<>();
    private final Map<String, TextureRegion[][]> regions = new ConcurrentHashMap<>();
    private final Map<String, GroundItem> groundItems = new ConcurrentHashMap<>();

    private float animationTime = 0;

    public void addItem(GroundItem item) {
        groundItems.put(item.getInstanceId(), item);
        logger.info("Item added to renderer: {} at ({}, {}) - Total items: {}",
                item.getDefinition().getName(),
                item.getX(),
                item.getY(),
                groundItems.size());
        loadSpritesheetIfNeeded(item.getDefinition().getSpritesheet());
    }

    public void removeItem(String instanceId) {
        groundItems.remove(instanceId);
        logger.debug("Item removed from renderer: {}", instanceId);
    }

    private void loadSpritesheetIfNeeded(String path) {
        if (spritesheets.containsKey(path)) return;

        logger.info("Loading spritesheet: {}", path);

        // Tentar diferentes caminhos
        String[] possiblePaths = {
                path,
                "assets/" + path,
                "client/assets/" + path,
                "../client/assets/" + path,
                "C:/Users/Marllon/IdeaProjects/sandbox-simulator/client/assets/" + path
        };

        Texture texture = null;
        for (String tryPath : possiblePaths) {
            try {
                FileHandle file = Gdx.files.internal(tryPath);
                if (file.exists()) {
                    texture = new Texture(file);
                    logger.info("Loaded spritesheet from: {}", tryPath);
                    break;
                } else {
                    logger.debug("File not found: {}", tryPath);
                }
            } catch (Exception e) {
                logger.debug("Failed to load from: {} - {}", tryPath, e.getMessage());
            }
        }

        if (texture == null) {
            logger.error("Could not load spritesheet from any path: {}", path);
            // Listar arquivos disponíveis para debug
            try {
                FileHandle[] files = Gdx.files.internal(".").list();
                logger.info("Files in internal directory:");
                for (FileHandle file : files) {
                    logger.info("  - {}", file.name());
                }
            } catch (Exception e) {
                logger.error("Could not list files", e);
            }
            return;
        }

        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        spritesheets.put(path, texture);
        int cols = texture.getWidth() / 32;
        int rows = texture.getHeight() / 32;
        regions.put(path, TextureRegion.split(texture, 32, 32));
        logger.info("Loaded spritesheet: {} ({}x{} tiles, {}x{} pixels)",
                path, cols, rows, texture.getWidth(), texture.getHeight());
    }

    public void update(float delta) {
        animationTime += delta;
    }

    public void render(SpriteBatch batch, GameCamera camera) {
        if (groundItems.isEmpty()) {
            return;
        }

        for (GroundItem item : groundItems.values()) {
            float x = item.getX();
            float y = item.getY();

            if (camera.isInView(x, y, 32, 32)) {
                TextureRegion[][] regs = regions.get(item.getDefinition().getSpritesheet());
                if (regs != null) {
                    int tx = item.getDefinition().getTileX();
                    int ty = item.getDefinition().getTileY();
                    if (tx >= 0 && tx < regs[0].length && ty >= 0 && ty < regs.length) {
                        TextureRegion region = regs[ty][tx];
                        float floatY = y + (float) Math.sin(animationTime * 3) * 4;
                        batch.draw(region, x - 16, floatY - 16, 32, 32);
                    } else {
                        logger.warn("Invalid tile coordinates: tx={}, ty={} for spritesheet {}",
                                tx, ty, item.getDefinition().getSpritesheet());
                    }
                } else {
                    logger.debug("No spritesheet regions for: {}", item.getDefinition().getSpritesheet());
                }
            }
        }
    }

    public void dispose() {
        for (Texture t : spritesheets.values()) t.dispose();
        spritesheets.clear();
        regions.clear();
        groundItems.clear();
        logger.info("ItemRenderer disposed");
    }
}