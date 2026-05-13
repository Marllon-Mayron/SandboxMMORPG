package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class PickupItemPacket extends Packet {
    public String instanceId;
    public String playerId;

    public PickupItemPacket() {}

    public PickupItemPacket(String instanceId, String playerId) {
        this.instanceId = instanceId;
        this.playerId = playerId;
    }
}