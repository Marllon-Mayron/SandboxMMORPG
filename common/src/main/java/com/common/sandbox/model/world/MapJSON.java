package com.common.sandbox.model.world;

import java.util.HashMap;
import java.util.Map;

public class MapJSON {
    private String mapId;
    private String mapName;
    private Map<String, ChunkData> chunks = new HashMap<>();

    public MapJSON() {}

    public MapJSON(String mapId, String mapName) {
        this.mapId = mapId;
        this.mapName = mapName;
    }

    // Getters e Setters
    public String getMapId() { return mapId; }
    public void setMapId(String mapId) { this.mapId = mapId; }
    public String getMapName() { return mapName; }
    public void setMapName(String mapName) { this.mapName = mapName; }
    public Map<String, ChunkData> getChunks() { return chunks; }
    public void setChunks(Map<String, ChunkData> chunks) { this.chunks = chunks; }
    public void addChunk(int x, int y, ChunkData chunk) { chunks.put(x + ":" + y, chunk); }

    public static class ChunkData {
        private int chunkX;
        private int chunkY;
        private TileData[][][] layers = new TileData[3][32][32];

        public ChunkData() {}

        public ChunkData(int chunkX, int chunkY) {
            this.chunkX = chunkX;
            this.chunkY = chunkY;
            // Inicializar com valores vazios
            for (int layer = 0; layer < 3; layer++) {
                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        layers[layer][x][y] = new TileData();
                    }
                }
            }
        }

        public void setTile(int layer, int x, int y, String spritesheetPath, int tileId, String tag) {
            if (layer >= 0 && layer < 3 && x >= 0 && x < 32 && y >= 0 && y < 32) {
                layers[layer][x][y] = new TileData(spritesheetPath, tileId, tag);
            }
        }

        public TileData getTile(int layer, int x, int y) {
            if (layer >= 0 && layer < 3 && x >= 0 && x < 32 && y >= 0 && y < 32) {
                return layers[layer][x][y];
            }
            return null;
        }

        // Getters/Setters
        public int getChunkX() { return chunkX; }
        public void setChunkX(int chunkX) { this.chunkX = chunkX; }
        public int getChunkY() { return chunkY; }
        public void setChunkY(int chunkY) { this.chunkY = chunkY; }
        public TileData[][][] getLayers() { return layers; }
        public void setLayers(TileData[][][] layers) { this.layers = layers; }

    }

    // Classe para armazenar tile completo
    public static class TileData {
        private String spritesheetPath;
        private int tileId;
        private String tag;

        public TileData() {
            this.spritesheetPath = "";
            this.tileId = 0;
            this.tag = "default";
        }

        public TileData(String spritesheetPath, int tileId) {
            this(spritesheetPath, tileId, "default");
        }

        public TileData(String spritesheetPath, int tileId, String tag) {
            this.spritesheetPath = spritesheetPath != null ? spritesheetPath : "";
            this.tileId = tileId;
            this.tag = tag != null ? tag : "default";
        }

        public String getSpritesheetPath() { return spritesheetPath; }
        public void setSpritesheetPath(String spritesheetPath) { this.spritesheetPath = spritesheetPath; }
        public int getTileId() { return tileId; }
        public void setTileId(int tileId) { this.tileId = tileId; }
        public String getTag() { return tag; }
        public void setTag(String tag) { this.tag = tag; }

        public boolean isEmpty() {
            return spritesheetPath == null || spritesheetPath.isEmpty() || tileId < 0;
        }

        //Métodos para verificar tipo de tile
        public boolean isSolid() {
            return "solid".equals(tag);
        }

        public boolean isWater() {
            return "water".equals(tag);
        }

        public boolean isLava() {
            return "lava".equals(tag);
        }

        public boolean isGrass() {
            return "grass".equals(tag);
        }

        public boolean isSand() {
            return "sand".equals(tag);
        }

        public boolean isIce() {
            return "ice".equals(tag);
        }

        public boolean isMud() {
            return "mud".equals(tag);
        }

        public float getWalkSpeed() {
            switch (tag) {
                case "water": return 0.5f;
                case "lava": return 0.4f;
                case "sand": return 0.8f;
                case "ice": return 1.2f;
                case "mud": return 0.6f;
                case "grass": return 1.0f;
                default: return 1.0f;
            }
        }

        public int getDamage() {
            switch (tag) {
                case "lava": return 10;
                case "water": return 0;
                default: return 0;
            }
        }

        @Override
        public String toString() {
            if (isEmpty()) return "TileData[EMPTY]";
            return "TileData[path=" + spritesheetPath + ", id=" + tileId + ", tag=" + tag + "]";
        }
    }
}