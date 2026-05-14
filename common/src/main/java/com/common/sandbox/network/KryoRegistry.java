package com.common.sandbox.network;

import com.common.sandbox.model.*;
import com.common.sandbox.network.packets.*;
import com.common.sandbox.network.packets.InventoryUpdatePacket;
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

    // REMOVIDOS: ID_MOVEMENT_REQUEST, ID_MOVEMENT_BROADCAST, ID_MOVEMENT_CONFIRM
    // NOVO ID para PlayerStatePacket
    private static final int ID_PLAYER_STATE = 24;

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
    private static final int ID_MAP_JSON = 62;
    private static final int ID_WORLD_TILE = 63;
    private static final int ID_TILE_TAG = 64;

    private static final int ID_MAP_JSON_CHUNK_DATA = 70;
    private static final int ID_HASH_MAP = 80;
    private static final int ID_LINKED_HASH_MAP = 81;
    private static final int ID_ARRAY_LIST = 82;

    private static final int ID_FRIEND_REQUEST = 90;
    private static final int ID_FRIEND_LIST_RESPONSE = 91;
    private static final int ID_PRIVATE_MESSAGE = 92;
    private static final int ID_FRIEND_STATUS_UPDATE = 93;

    private static final int ID_PRIVATE_MESSAGE_HISTORY_REQUEST = 96;
    private static final int ID_PRIVATE_MESSAGE_HISTORY_RESPONSE = 97;

    private static final int ID_ITEM_DEFINITION = 160;
    private static final int ID_GROUND_ITEM = 161;
    private static final int ID_ITEM_SPAWN_PACKET = 162;
    private static final int ID_ITEM_DESPAWN_PACKET = 163;

    private static final int ID_INVENTORY = 170;
    private static final int ID_ITEM_STACK = 171;
    private static final int ID_INVENTORY_UPDATE = 172;
    private static final int ID_PICKUP_ITEM = 173;
    private static final int ID_DROP_ITEM = 174;
    private static final int ID_PICKUP_RESULT = 175;

    private static final int ID_ATTACK_REQUEST = 180;
    private static final int ID_ATTACK_BROADCAST = 181;
    private static final int ID_DAMAGE_PACKET = 182;
    private static final int ID_ATTACK_TYPE = 183;
    private static final int ID_ATTACK_RESULT = 184;
    private static final int ID_COMBAT_STATS = 185;

    private static boolean registered = false;

    public static synchronized void registerClasses(Kryo kryo) {
        // Authentication
        kryo.register(LoginRequest.class, ID_LOGIN_REQUEST);
        kryo.register(LoginResponse.class, ID_LOGIN_RESPONSE);
        kryo.register(RegisterRequest.class, ID_REGISTER_REQUEST);
        kryo.register(RegisterResponse.class, ID_REGISTER_RESPONSE);

        kryo.register(PlayerStatePacket.class, ID_PLAYER_STATE);
        kryo.register(PlayerLeftPacket.class, 22);

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

        // Models
        kryo.register(Player.class, ID_PLAYER);
        kryo.register(Chunk.class, ID_CHUNK);
        kryo.register(MapJSON.class, ID_MAP_JSON);
        kryo.register(MapJSON.ChunkData.class, ID_MAP_JSON_CHUNK_DATA);
        kryo.register(WorldTile.class, ID_WORLD_TILE);
        kryo.register(TileTag.class, ID_TILE_TAG);

        // Collections
        kryo.register(HashMap.class, ID_HASH_MAP);
        kryo.register(LinkedHashMap.class, ID_LINKED_HASH_MAP);
        kryo.register(ArrayList.class, ID_ARRAY_LIST);

        // Friend system
        kryo.register(FriendRequestPacket.class, ID_FRIEND_REQUEST);
        kryo.register(FriendListResponse.class, ID_FRIEND_LIST_RESPONSE);
        kryo.register(PrivateMessagePacket.class, ID_PRIVATE_MESSAGE);
        kryo.register(FriendListResponse.FriendInfo.class, 94);
        kryo.register(FriendListResponse.FriendRequestInfo.class, 95);

        kryo.register(ItemDefinition.class, ID_ITEM_DEFINITION);
        kryo.register(GroundItem.class, ID_GROUND_ITEM);
        kryo.register(ItemSpawnPacket.class, ID_ITEM_SPAWN_PACKET);
        kryo.register(ItemDespawnPacket.class, ID_ITEM_DESPAWN_PACKET);

        // Private message history
        kryo.register(PrivateMessageHistoryRequest.class, ID_PRIVATE_MESSAGE_HISTORY_REQUEST);
        kryo.register(PrivateMessageHistoryResponse.class, ID_PRIVATE_MESSAGE_HISTORY_RESPONSE);

        kryo.register(Inventory.class, ID_INVENTORY);
        kryo.register(ItemStack.class, ID_ITEM_STACK);
        kryo.register(InventoryUpdatePacket.class, ID_INVENTORY_UPDATE);
        kryo.register(PickupItemPacket.class, ID_PICKUP_ITEM);
        kryo.register(DropItemPacket.class, ID_DROP_ITEM);
        kryo.register(PickupResultPacket.class, ID_PICKUP_RESULT);

        // Arrays multidimensionais
        kryo.register(int[][].class, 90);
        kryo.register(int[][][].class, 91);
        kryo.register(boolean[][].class, 92);
        kryo.register(boolean[][][].class, 93);

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

        //hashmap
        kryo.register(java.util.HashMap.class, 179);

        kryo.register(AttackRequest.class, ID_ATTACK_REQUEST);
        kryo.register(AttackBroadcast.class, ID_ATTACK_BROADCAST);
        kryo.register(DamagePacket.class, ID_DAMAGE_PACKET);
        kryo.register(AttackType.class, ID_ATTACK_TYPE);
        kryo.register(AttackResult.class, ID_ATTACK_RESULT);
        kryo.register(CombatStats.class, ID_COMBAT_STATS);

        // IMPORTANTE: Permitir registro automático para classes não listadas
        kryo.setRegistrationRequired(false);

        registered = true;
    }

    public static void reset() {
        registered = false;
    }
}