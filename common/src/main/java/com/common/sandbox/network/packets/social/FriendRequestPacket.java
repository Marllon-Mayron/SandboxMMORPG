package com.common.sandbox.network.packets.social;

import com.common.sandbox.network.Packet;

public class FriendRequestPacket extends Packet {
    public String action; // SEND, ACCEPT, REJECT, REMOVE, LIST, NEW_REQUEST, ACCEPTED, REJECTED, REMOVED, SENT, ERROR
    public String targetUsername;
    public String requestId;
    public boolean success;
    public String message;
    public String fromPlayerId;
    public String fromUsername;
    public int fromLevel;

    public FriendRequestPacket() {}

    public FriendRequestPacket(String action, String targetUsername) {
        this.action = action;
        this.targetUsername = targetUsername;
        this.success = false;
    }
}