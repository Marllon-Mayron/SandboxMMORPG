package com.common.sandbox.network.packets;

import com.common.sandbox.model.MapJSON;

public class MapLoadResponse {
    public boolean success;
    public String message;
    public MapJSON mapJson;

    public MapLoadResponse() {}

    public MapLoadResponse(boolean success, String message, MapJSON mapJson) {
        this.success = success;
        this.message = message;
        this.mapJson = mapJson;
    }
}