package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class MapLoadRequest extends Packet {
    public String mapId;

    public MapLoadRequest() {}

    public MapLoadRequest(String mapId) {
        this.mapId = mapId;
    }
}