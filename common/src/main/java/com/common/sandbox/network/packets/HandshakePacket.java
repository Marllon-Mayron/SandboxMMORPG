package com.common.sandbox.network.packets;

import java.io.Serializable;

public class HandshakePacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public int version;
    public String clientId;
    public long timestamp;

    public HandshakePacket() {
        this.timestamp = System.currentTimeMillis();
        this.version = 1;
        this.clientId = "Unknown";
    }

    public HandshakePacket(int version, String clientId) {
        this.version = version;
        this.clientId = clientId;
        this.timestamp = System.currentTimeMillis();
    }
}