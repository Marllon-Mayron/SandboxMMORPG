package com.common.sandbox.model.item;

import java.io.Serializable;

public class GroundItem implements Serializable {
    private static final long serialVersionUID = 1L;

    private String instanceId;      // ID único para cada item no chão (UUID)
    private ItemDefinition definition; // O tipo do item
    private float x;                // Posição X no mundo
    private float y;                // Posição Y no mundo
    private long spawnTime;         // Timestamp de quando apareceu
    private int despawnSeconds;     // Tempo de vida em segundos (ex: 60)

    public GroundItem() {}

    public GroundItem(String instanceId, ItemDefinition definition, float x, float y, int despawnSeconds) {
        this.instanceId = instanceId;
        this.definition = definition;
        this.x = x;
        this.y = y;
        this.spawnTime = System.currentTimeMillis();
        this.despawnSeconds = despawnSeconds;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime > despawnSeconds * 1000L;
    }

    public String getInstanceId() { return instanceId; }
    public void setInstanceId(String instanceId) { this.instanceId = instanceId; }
    public ItemDefinition getDefinition() { return definition; }
    public void setDefinition(ItemDefinition definition) { this.definition = definition; }
    public float getX() { return x; }
    public void setX(float x) { this.x = x; }
    public float getY() { return y; }
    public void setY(float y) { this.y = y; }
    public long getSpawnTime() { return spawnTime; }
    public void setSpawnTime(long spawnTime) { this.spawnTime = spawnTime; }
    public int getDespawnSeconds() { return despawnSeconds; }
    public void setDespawnSeconds(int despawnSeconds) { this.despawnSeconds = despawnSeconds; }
}