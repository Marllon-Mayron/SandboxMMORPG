package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;
import com.common.sandbox.model.Player;
import java.util.List;

public class MovementBroadcast extends Packet {
    public Player player;
    public List<Player> allPlayers; // Para sincronização inicial

    public MovementBroadcast() {}

    public MovementBroadcast(Player player) {
        this.player = player;
    }

    public MovementBroadcast(Player player, List<Player> allPlayers) {
        this.player = player;
        this.allPlayers = allPlayers;
    }
}