package com.sandbox.server;

import com.common.sandbox.model.MapJSON;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {
    private static final Logger logger = LoggerFactory.getLogger(ChunkManager.class);
    private static ChunkManager instance;

    private static final String WORLD_MAP_ID = "11111111-1111-1111-1111-111111111111";
    private static final String WORLD_MAP_NAME = "sandbox_world";

    private final ConcurrentHashMap<String, MapJSON> loadedMaps;
    private final ObjectMapper objectMapper;
    private String currentMapId = WORLD_MAP_ID;

    private ChunkManager() {
        this.loadedMaps = new ConcurrentHashMap<>();
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        ensureWorldMapExists();
    }

    public static synchronized ChunkManager getInstance() {
        if (instance == null) {
            instance = new ChunkManager();
        }
        return instance;
    }

    private void ensureWorldMapExists() {
        String checkSql = "SELECT id FROM maps WHERE id = ?::uuid";
        String insertSql = "INSERT INTO maps (id, name, map_data, created_by, is_active) VALUES (?::uuid, ?, ?::jsonb, 'system', true)";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {

            checkStmt.setString(1, WORLD_MAP_ID);
            ResultSet rs = checkStmt.executeQuery();

            if (!rs.next()) {
                MapJSON emptyMap = new MapJSON(WORLD_MAP_ID, WORLD_MAP_NAME);
                String jsonData = objectMapper.writeValueAsString(emptyMap);

                try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                    insertStmt.setString(1, WORLD_MAP_ID);
                    insertStmt.setString(2, WORLD_MAP_NAME);
                    insertStmt.setString(3, jsonData);
                    insertStmt.executeUpdate();
                    logger.info("Created new world map: {}", WORLD_MAP_ID);
                }
            } else {
                logger.info("World map exists: {}", WORLD_MAP_ID);
            }

        } catch (Exception e) {
            logger.error("Failed to ensure world map exists", e);
        }
    }

    public MapJSON getMap() {
        return getMap(currentMapId);
    }

    public MapJSON getMap(String mapId) {
        if (loadedMaps.containsKey(mapId)) {
            return loadedMaps.get(mapId);
        }

        String sql = "SELECT map_data FROM maps WHERE id = ?::uuid";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, mapId);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                String jsonData = rs.getString("map_data");
                MapJSON map = objectMapper.readValue(jsonData, MapJSON.class);
                loadedMaps.put(mapId, map);
                logger.info("Loaded map {} from database with {} chunks", mapId, map.getChunks().size());
                return map;
            }

        } catch (Exception e) {
            logger.error("Failed to load map {} from database", mapId, e);
        }

        return null;
    }

    public void saveMap(MapJSON map) {
        String sql = "UPDATE maps SET map_data = ?::jsonb, updated_at = CURRENT_TIMESTAMP WHERE id = ?::uuid";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String jsonData = objectMapper.writeValueAsString(map);
            pstmt.setString(1, jsonData);
            pstmt.setString(2, map.getMapId());
            pstmt.executeUpdate();

            loadedMaps.put(map.getMapId(), map);
            logger.info("Saved map {} to database with {} chunks", map.getMapId(), map.getChunks().size());

        } catch (Exception e) {
            logger.error("Failed to save map", e);
        }
    }

    /**
     * Atualiza um tile no mapa usando a nova estrutura TileData COM TAG
     */
    public void updateTile(int chunkX, int chunkY, int localX, int localY, int layer, String spritesheetPath, int tileId, String tag) {
        MapJSON map = getMap();
        if (map == null) return;

        String key = chunkX + ":" + chunkY;
        MapJSON.ChunkData chunk = map.getChunks().get(key);

        if (chunk == null) {
            chunk = new MapJSON.ChunkData(chunkX, chunkY);
            map.addChunk(chunkX, chunkY, chunk);
        }

        if (layer >= 0 && layer < 3 && localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            chunk.setTile(layer, localX, localY, spritesheetPath, tileId, tag);
        }

        saveMap(map);
    }


    /**
     * Obtém um tile do mapa retornando TileData
     */
    public MapJSON.TileData getTileData(int chunkX, int chunkY, int localX, int localY, int layer) {
        MapJSON map = getMap();
        if (map == null) return null;

        String key = chunkX + ":" + chunkY;
        MapJSON.ChunkData chunk = map.getChunks().get(key);

        if (chunk == null) return null;
        if (layer >= 0 && layer < 3 && localX >= 0 && localX < 32 && localY >= 0 && localY < 32) {
            return chunk.getTile(layer, localX, localY);
        }
        return null;
    }

    /**
     * Verifica se uma posição global é sólida (impede movimento)
     * Usa o tamanho de tile 64x64 (2x)
     */
    public boolean isSolid(float worldX, float worldY) {
        int chunkSize = 32;
        int tileSize = 64;

        int chunkX = (int) Math.floor(worldX / (chunkSize * tileSize));
        int chunkY = (int) Math.floor(worldY / (chunkSize * tileSize));
        int localX = (int) (worldX % (chunkSize * tileSize)) / tileSize;
        int localY = (int) (worldY % (chunkSize * tileSize)) / tileSize;

        if (localX < 0) localX += chunkSize;
        if (localY < 0) localY += chunkSize;

        if (localX < 0 || localX >= chunkSize || localY < 0 || localY >= chunkSize) {
            return true;
        }

        MapJSON map = getMap();
        if (map == null) return true;

        String key = chunkX + ":" + chunkY;
        MapJSON.ChunkData chunk = map.getChunks().get(key);
        if (chunk == null) return false;

        MapJSON.TileData tileData = chunk.getTile(0, localX, localY);

        // ✅ SÓ É SÓLIDO SE A TAG FOR "solid"
        if (tileData != null && !tileData.isEmpty()) {
            return "solid".equals(tileData.getTag());
        }

        return false;
    }

    public String getCurrentMapId() {
        return currentMapId;
    }

    public void close() {
        for (MapJSON map : loadedMaps.values()) {
            saveMap(map);
        }
        logger.info("ChunkManager closed, saved {} maps", loadedMaps.size());
    }
}