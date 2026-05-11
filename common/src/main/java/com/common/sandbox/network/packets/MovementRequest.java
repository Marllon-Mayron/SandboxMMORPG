package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class MovementRequest extends Packet {
    public String playerId;  // String
    public float x, y;
    public String direction;

    public MovementRequest() {}

    public MovementRequest(String playerId, float x, float y, String direction) {
        this.playerId = playerId;
        this.x = x;
        this.y = y;
        this.direction = direction;
    }
}