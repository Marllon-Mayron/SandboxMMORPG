package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class LoginRequest extends Packet {
    public String username;
    public String password;

    public LoginRequest() {}

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }
}