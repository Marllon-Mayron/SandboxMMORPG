package com.common.sandbox.network.packets;

import com.common.sandbox.model.Player;

public class MovementConfirm extends com.common.sandbox.network.Packet {
    public Player player;

    public MovementConfirm() {}

    public MovementConfirm(Player player) {
        this.player = player;
    }
}