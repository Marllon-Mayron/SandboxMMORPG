package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class PrivateMessagePacket extends Packet {
    public String fromPlayerId;
    public String fromUsername;
    public String toPlayerId;
    public String toUsername;
    public String message;
    public long timestamp;

    public PrivateMessagePacket() {}

    public PrivateMessagePacket(String fromPlayerId, String fromUsername, String toPlayerId, String toUsername, String message) {
        this.fromPlayerId = fromPlayerId;
        this.fromUsername = fromUsername;
        this.toPlayerId = toPlayerId;
        this.toUsername = toUsername;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}