package com.common.sandbox.network.packets.world;

import com.common.sandbox.model.world.MapJSON;

public class MapSaveRequest {
    public MapJSON mapData;

    public MapSaveRequest() {}

    public MapSaveRequest(MapJSON mapData) {
        this.mapData = mapData;
    }
}