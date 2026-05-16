package com.common.sandbox.network.packets.chat;

import com.common.sandbox.network.Packet;

public class ChatMessage extends Packet {
    public String senderId;
    public String senderName;
    public String message;
    public long timestamp;

    public ChatMessage() {}

    public ChatMessage(String senderId, String senderName, String message) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }
}