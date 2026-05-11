package com.common.sandbox.network;

import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.*;
import com.esotericsoftware.kryo.Kryo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.UUID;

public class KryoRegistry {

    // IDs fixos para garantir consistência entre servidor e cliente
    private static final int ID_LOGIN_REQUEST = 10;
    private static final int ID_LOGIN_RESPONSE = 11;
    private static final int ID_REGISTER_REQUEST = 12;
    private static final int ID_REGISTER_RESPONSE = 13;

    private static final int ID_MOVEMENT_REQUEST = 20;
    private static final int ID_MOVEMENT_BROADCAST = 21;

    private static final int ID_CHAT_MESSAGE = 30;

    private static final int ID_HANDSHAKE = 40;
    private static final int ID_PING = 41;

    private static final int ID_CHUNK_UPDATE = 50;
    private static final int ID_MAP_SAVE_REQUEST = 51;
    private static final int ID_MAP_SAVE_RESPONSE = 52;
    private static final int ID_MAP_LOAD_REQUEST = 53;
    private static final int ID_MAP_LOAD_RESPONSE = 54;

    private static final int ID_PLAYER = 60;
    private static final int ID_CHUNK = 61;
    private static final int ID_MAP_JSON = 62;  // Mudado de ID_MAP_DATA para ID_MAP_JSON
    private static final int ID_WORLD_TILE = 63;
    private static final int ID_TILE_TAG = 64;

    // Novos IDs para classes do JSON
    private static final int ID_MAP_JSON_CHUNK_DATA = 70;

    private static final int ID_HASH_MAP = 80;
    private static final int ID_LINKED_HASH_MAP = 81;
    private static final int ID_ARRAY_LIST = 82;

    private static boolean registered = false;

    public static synchronized void registerClasses(Kryo kryo) {
        // Authentication
        kryo.register(LoginRequest.class, ID_LOGIN_REQUEST);
        kryo.register(LoginResponse.class, ID_LOGIN_RESPONSE);
        kryo.register(RegisterRequest.class, ID_REGISTER_REQUEST);
        kryo.register(RegisterResponse.class, ID_REGISTER_RESPONSE);

        // Movement
        kryo.register(MovementRequest.class, ID_MOVEMENT_REQUEST);
        kryo.register(MovementBroadcast.class, ID_MOVEMENT_BROADCAST);

        // Chat
        kryo.register(ChatMessage.class, ID_CHAT_MESSAGE);

        // Handshake & Ping
        kryo.register(HandshakePacket.class, ID_HANDSHAKE);
        kryo.register(PingPacket.class, ID_PING);

        // Map packets
        kryo.register(ChunkUpdatePacket.class, ID_CHUNK_UPDATE);
        kryo.register(MapSaveRequest.class, ID_MAP_SAVE_REQUEST);
        kryo.register(MapSaveResponse.class, ID_MAP_SAVE_RESPONSE);
        kryo.register(MapLoadRequest.class, ID_MAP_LOAD_REQUEST);
        kryo.register(MapLoadResponse.class, ID_MAP_LOAD_RESPONSE);

        // Models - Usando MAP_JSON (nova classe)
        kryo.register(Player.class, ID_PLAYER);
        kryo.register(Chunk.class, ID_CHUNK);
        kryo.register(MapJSON.class, ID_MAP_JSON);  // NOVO: MapJSON
        kryo.register(MapJSON.ChunkData.class, ID_MAP_JSON_CHUNK_DATA);  // NOVO: Classe interna
        kryo.register(WorldTile.class, ID_WORLD_TILE);
        kryo.register(TileTag.class, ID_TILE_TAG);

        // Collections
        kryo.register(HashMap.class, ID_HASH_MAP);
        kryo.register(LinkedHashMap.class, ID_LINKED_HASH_MAP);
        kryo.register(ArrayList.class, ID_ARRAY_LIST);

        // Arrays multidimensionais importantes para o MapJSON
        kryo.register(int[][].class, 90);
        kryo.register(int[][][].class, 91);  // 3D array para layers
        kryo.register(boolean[][].class, 92);
        kryo.register(boolean[][][].class, 93);  // 3D array para solid

        // Arrays básicos
        kryo.register(WorldTile[].class, 100);
        kryo.register(WorldTile[][].class, 101);
        kryo.register(int[].class, 102);
        kryo.register(float[].class, 103);
        kryo.register(float[][].class, 104);
        kryo.register(String[].class, 105);
        kryo.register(String[][].class, 106);
        kryo.register(byte[].class, 107);
        kryo.register(byte[][].class, 108);
        kryo.register(boolean[].class, 109);
        kryo.register(boolean[][].class, 110);
        kryo.register(long[].class, 111);
        kryo.register(double[].class, 112);
        kryo.register(Object[].class, 113);

        // Basic types
        kryo.register(String.class, 120);
        kryo.register(Integer.class, 121);
        kryo.register(Float.class, 122);
        kryo.register(Boolean.class, 123);
        kryo.register(Long.class, 124);
        kryo.register(Double.class, 125);
        kryo.register(UUID.class, 126);

        // Primitive types
        kryo.register(int.class, 130);
        kryo.register(float.class, 131);
        kryo.register(boolean.class, 132);
        kryo.register(long.class, 133);
        kryo.register(double.class, 134);
        kryo.register(byte.class, 135);
        kryo.register(char.class, 136);
        kryo.register(short.class, 137);

        // Empty collections
        kryo.register(java.util.Collections.EMPTY_MAP.getClass(), 140);
        kryo.register(java.util.Collections.EMPTY_LIST.getClass(), 141);
        kryo.register(java.util.Collections.EMPTY_SET.getClass(), 142);

        // Date/Time
        kryo.register(java.util.Date.class, 150);
        kryo.register(java.sql.Timestamp.class, 151);

        // IMPORTANTE: Permitir registro automático para classes não listadas
        kryo.setRegistrationRequired(false);

        registered = true;
    }

    public static void reset() {
        registered = false;
    }
}