package com.common.sandbox.network.packets.chat;

import com.common.sandbox.network.Packet;
import java.util.List;

public class PrivateMessageHistoryResponse extends Packet {
    public String friendId;
    public List<PrivateMessagePacket> messages;

    public PrivateMessageHistoryResponse() {}

    public PrivateMessageHistoryResponse(String friendId, List<PrivateMessagePacket> messages) {
        this.friendId = friendId;
        this.messages = messages;
    }
}