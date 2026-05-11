package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class MapSaveResponse extends Packet {
    public boolean success;
    public String message;
    public String mapId;

    public MapSaveResponse() {}

    public MapSaveResponse(boolean success, String message, String mapId) {
        this.success = success;
        this.message = message;
        this.mapId = mapId;
    }
}