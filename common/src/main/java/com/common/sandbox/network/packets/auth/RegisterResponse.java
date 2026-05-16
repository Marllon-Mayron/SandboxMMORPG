package com.common.sandbox.network.packets.auth;

import com.common.sandbox.network.Packet;

public class RegisterResponse extends Packet {
    public boolean success;
    public String message;

    public RegisterResponse() {}

    public RegisterResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
}