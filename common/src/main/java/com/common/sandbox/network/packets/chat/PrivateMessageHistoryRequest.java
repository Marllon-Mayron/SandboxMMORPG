package com.common.sandbox.network.packets.chat;

import com.common.sandbox.network.Packet;

public class PrivateMessageHistoryRequest extends Packet {
    public String friendId;
    public int limit;

    public PrivateMessageHistoryRequest() {}

    public PrivateMessageHistoryRequest(String friendId, int limit) {
        this.friendId = friendId;
        this.limit = limit;
    }
}