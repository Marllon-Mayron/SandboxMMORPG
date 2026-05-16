package com.common.sandbox.network.packets.connection;

import java.io.Serializable;

public class PingPacket implements Serializable {
    private static final long serialVersionUID = 1L;

    public long timestamp;

    public PingPacket() {
        this.timestamp = System.currentTimeMillis();
    }
}