package com.common.sandbox.network.packets;

import com.common.sandbox.model.MapJSON;

public class MapSaveRequest {
    public MapJSON mapData;

    public MapSaveRequest() {}

    public MapSaveRequest(MapJSON mapData) {
        this.mapData = mapData;
    }
}