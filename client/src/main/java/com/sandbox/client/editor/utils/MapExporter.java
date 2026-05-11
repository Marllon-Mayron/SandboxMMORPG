package com.sandbox.client.editor.utils;

import com.common.sandbox.model.MapJSON;
import com.sandbox.client.SandboxClient;
import com.sandbox.client.editor.models.ChunkData;
import com.sandbox.client.editor.models.EditorState;
import com.sandbox.client.editor.models.LayerType;
import com.sandbox.client.editor.models.TileRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapExporter {
    private static final Logger logger = LoggerFactory.getLogger(MapExporter.class);
    private static final String WORLD_MAP_ID = "11111111-1111-1111-1111-111111111111";
    private final SandboxClient game;

    public MapExporter(SandboxClient game) {
        this.game = game;
    }

    public void saveMap(EditorState state) {
        MapJSON map = new MapJSON(WORLD_MAP_ID, "sandbox_world");

        int totalTilesSaved = 0;

        for (ChunkData editorChunk : state.getChunks().values()) {
            MapJSON.ChunkData chunk = new MapJSON.ChunkData(editorChunk.getX(), editorChunk.getY());

            int tilesInChunk = 0;

            for (LayerType layer : LayerType.values()) {
                for (int x = 0; x < 32; x++) {
                    for (int y = 0; y < 32; y++) {
                        TileRef tile = editorChunk.getTile(layer, x, y);
                        if (tile != null && tile.isValid()) {
                            String spritesheetPath = tile.getSpritesheetPath();
                            if (spritesheetPath == null || spritesheetPath.isEmpty()) {
                                spritesheetPath = "world/outside.png";
                            }

                            //SALVAR A TAG DO TILE
                            String tagName = tile.getTag() != null ? tile.getTag().getName() : "default";

                            chunk.setTile(layer.id, x, y, spritesheetPath, tile.getTileId(), tagName);
                            tilesInChunk++;
                            totalTilesSaved++;

                            logger.debug("Saved tile layer={} ({},{}): path={}, id={}, tag={}",
                                    layer.name, x, y, spritesheetPath, tile.getTileId(), tagName);
                        }
                    }
                }
            }

            map.addChunk(editorChunk.getX(), editorChunk.getY(), chunk);
            logger.info("Chunk [{},{}] saved with {} tiles", editorChunk.getX(), editorChunk.getY(), tilesInChunk);
        }

        logger.info("Saving map with {} chunks, total {} tiles", state.getChunks().size(), totalTilesSaved);

        if (game.getNetworkClient() != null) {
            game.getNetworkClient().sendMapSave(map);
            logger.info("Map save request sent to server");
        }
    }
}