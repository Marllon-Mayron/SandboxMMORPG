package com.common.sandbox.network.packets.player;

import com.common.sandbox.network.Packet;

public class PlayerLeftPacket extends Packet {
    public String playerId;
    public String playerName;

    public PlayerLeftPacket() {}

    public PlayerLeftPacket(String playerId, String playerName) {
        this.playerId = playerId;
        this.playerName = playerName;
    }
}