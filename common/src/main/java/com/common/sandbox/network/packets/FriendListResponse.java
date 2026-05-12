package com.common.sandbox.network.packets;

import com.common.sandbox.network.Packet;
import java.util.List;
import java.util.Map;

public class FriendListResponse extends Packet {
    public List<FriendInfo> friends;
    public List<FriendRequestInfo> pendingRequests;

    public FriendListResponse() {}

    public static class FriendInfo {
        public String playerId;
        public String username;
        public int level;
        public boolean isOnline;

        public FriendInfo() {}
    }

    public static class FriendRequestInfo {
        public String requestId;
        public String fromPlayerId;
        public String fromUsername;
        public int fromLevel;
        public long createdAt;

        public FriendRequestInfo() {}
    }
}