package com.common.sandbox.network;

/**
 * Tipos de pacotes para identificar mensagens
 */
public enum PacketType {
    // Autenticação
    LOGIN_REQUEST(1),
    LOGIN_RESPONSE(2),
    REGISTER_REQUEST(3),
    REGISTER_RESPONSE(4),

    // Handshake
    HANDSHAKE(5),

    // Movimento
    MOVEMENT_REQUEST(10),
    MOVEMENT_BROADCAST(11),

    // Chat
    CHAT_MESSAGE(20),
    CHAT_HISTORY(21),

    // Mapa e Chunks
    CHUNK_DATA(30),
    CHUNK_UPDATE(31),
    CHUNK_REQUEST(32),

    // Salvamento de Mapa
    MAP_SAVE_REQUEST(40),
    MAP_SAVE_RESPONSE(41),
    MAP_LOAD_REQUEST(42),
    MAP_LOAD_RESPONSE(43),

    // Tags
    TILE_TAG_UPDATE(50),
    TILE_TAG_REQUEST(51),

    // Heartbeat
    PING(99),
    PONG(100);

    private final int id;

    PacketType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public static PacketType fromId(int id) {
        for (PacketType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return null;
    }
}