package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;

public class RegisterRequest extends Packet {
    public String username;
    public String email;
    public String password;

    public RegisterRequest() {}

    public RegisterRequest(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }
}