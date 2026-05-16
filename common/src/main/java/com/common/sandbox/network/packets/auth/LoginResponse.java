package com.common.sandbox.network.packets.auth;

import com.common.sandbox.network.Packet;
import com.common.sandbox.model.player.Player;
import java.util.Map;

public class LoginResponse extends Packet {
    public boolean success;
    public String message;
    public Player player;
    public Map<String, Player> nearbyPlayers;

    public LoginResponse() {}

    public LoginResponse(boolean success, String message, Player player) {
        this.success = success;
        this.message = message;
        this.player = player;
    }
}