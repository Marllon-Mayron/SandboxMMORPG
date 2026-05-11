package com.common.sandbox.model;

import java.io.Serializable;

public class Player implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String username;
    private String email;
    private float x;
    private float y;
    private String direction;
    private Integer lastChunkX;
    private Integer lastChunkY;

    // Construtor padrão (OBRIGATÓRIO para Kryo)
    public Player() {
        this.id = "";
        this.username = "";
        this.email = "";
        this.x = 400;
        this.y = 300;
        this.direction = "DOWN";
    }

    public Player(String id, String username, String email) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.x = 400;
        this.y = 300;
        this.direction = "DOWN";
    }

    // Getters e Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public float getX() { return x; }
    public void setX(float x) { this.x = x; }

    public float getY() { return y; }
    public void setY(float y) { this.y = y; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public Integer getLastChunkX() { return lastChunkX; }
    public void setLastChunkX(Integer lastChunkX) { this.lastChunkX = lastChunkX; }

    public Integer getLastChunkY() { return lastChunkY; }
    public void setLastChunkY(Integer lastChunkY) { this.lastChunkY = lastChunkY; }
}