package com.sandbox.client;

import com.common.sandbox.model.MapJSON;
import com.common.sandbox.network.KryoRegistry;
import com.common.sandbox.network.packets.InventoryUpdatePacket;
import com.common.sandbox.network.packets.PingPacket;
import com.common.sandbox.network.packets.*;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.common.sandbox.model.Chunk;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class NetworkClient {
    private static final Logger logger = LoggerFactory.getLogger(NetworkClient.class);

    private final String host;
    private final int port;
    private EventLoopGroup group;
    private Channel channel;
    private Kryo kryo;

    private Consumer<LoginResponse> loginCallback;
    private Consumer<RegisterResponse> registerCallback;
    private Consumer<MovementBroadcast> movementCallback;
    private Consumer<ChatMessage> chatCallback;
    private Consumer<ChunkUpdatePacket> chunkUpdateCallback;
    private Consumer<MapSaveResponse> mapSaveCallback;
    private Consumer<MapLoadResponse> mapLoadCallback;
    private Consumer<Chunk> chunkCallback;
    private Consumer<PlayerLeftPacket> playerLeftCallback;
    private Consumer<FriendListResponse> friendListCallback;
    private Consumer<FriendRequestPacket> friendRequestCallback;
    private Consumer<PrivateMessagePacket> privateMessageCallback;
    private Consumer<PrivateMessageHistoryResponse> privateMessageHistoryCallback;
    private Consumer<InventoryUpdatePacket> inventoryCallback;
    private Consumer<PickupResultPacket> pickupResultCallback;
    private Consumer<ItemSpawnPacket> itemSpawnCallback;
    private Consumer<ItemDespawnPacket> itemDespawnCallback;

    public NetworkClient(String host, int port) {
        this.host = host;
        this.port = port;
        this.kryo = new Kryo();
        KryoRegistry.registerClasses(kryo);
        logger.info("NetworkClient Kryo initialized with unified registry");
    }

    public void connect() {
        group = new NioEventLoopGroup();

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));
                            pipeline.addLast(new ClientCodec());
                            pipeline.addLast(new ClientHandler());
                        }
                    });

            ChannelFuture future = bootstrap.connect(host, port).sync();
            channel = future.channel();
            logger.info("Connected to server {}:{}", host, port);

            sendHandshake();
            startHeartbeat();

        } catch (InterruptedException e) {
            logger.error("Failed to connect to server", e);
            Thread.currentThread().interrupt();
        }
    }

    private class ClientCodec extends ByteToMessageCodec<Object> {
        @Override
        protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
            try {
                ByteBufOutputStream bos = new ByteBufOutputStream(out);
                Output output = new Output(bos);
                kryo.writeClassAndObject(output, msg);
                output.flush();
                output.close();
                logger.debug("Encoded: {}", msg.getClass().getSimpleName());
            } catch (Exception e) {
                logger.error("Serialization error for {}: {}", msg.getClass().getSimpleName(), e.getMessage());
                throw e;
            }
        }

        @Override
        protected void decode(ChannelHandlerContext ctx, ByteBuf in, java.util.List<Object> out) throws Exception {
            if (in.readableBytes() < 4) {
                return;
            }

            in.markReaderIndex();
            try {
                ByteBufInputStream bis = new ByteBufInputStream(in);
                Input input = new Input(bis);
                Object obj = kryo.readClassAndObject(input);
                if (obj != null) {
                    logger.debug("Decoded: {}", obj.getClass().getSimpleName());
                    out.add(obj);
                }
                input.close();
            } catch (Exception e) {
                in.resetReaderIndex();
                logger.error("Deserialization error: {}", e.getMessage());
                int skipBytes = Math.min(in.readableBytes(), 1024);
                in.skipBytes(skipBytes);
            }
        }
    }

    private void sendHandshake() {
        HandshakePacket handshake = new HandshakePacket();
        handshake.version = 1;
        handshake.clientId = "SandboxClient";
        sendPacket(handshake);
        logger.info("Handshake sent to server");
    }

    private void startHeartbeat() {
        Thread.startVirtualThread(() -> {
            while (channel != null && channel.isActive()) {
                try {
                    Thread.sleep(5000);
                    sendPing();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    private void sendPing() {
        if (channel != null && channel.isActive()) {
            sendPacket(new PingPacket());
        }
    }

    public void sendLogin(String username, String password) {
        logger.info("Sending LOGIN - User: {}", username);
        LoginRequest request = new LoginRequest(username, password);
        sendPacket(request);
    }

    public void sendRegister(String username, String email, String password) {
        logger.info("Sending REGISTER - User: {}, Email: {}", username, email);
        RegisterRequest request = new RegisterRequest(username, email, password);
        sendPacket(request);
    }

    public void sendMovement(String playerId, float x, float y, String direction) {
        MovementRequest request = new MovementRequest(playerId, x, y, direction);
        sendPacket(request);
    }

    public void sendChat(String playerId, String playerName, String message) {
        ChatMessage chat = new ChatMessage(playerId, playerName, message);
        sendPacket(chat);
    }

    public void sendMapSave(MapJSON mapData) {
        logger.info("Sending MAP SAVE - Map: {}, chunks: {}", mapData.getMapName(), mapData.getChunks().size());
        MapSaveRequest request = new MapSaveRequest(mapData);
        sendPacket(request);
    }

    public void requestMapLoad(String mapId) {
        logger.info("Requesting MAP LOAD - Map ID: {}", mapId);
        MapLoadRequest request = new MapLoadRequest(mapId);
        sendPacket(request);
    }

    public void requestPrivateMessageHistory(String friendId, int limit) {
        PrivateMessageHistoryRequest request = new PrivateMessageHistoryRequest(friendId, limit);
        sendPacket(request);
    }

    public void sendPacket(Object packet) {
        if (channel == null || !channel.isActive()) {
            logger.warn("Channel not active, cannot send packet: {}", packet.getClass().getSimpleName());
            return;
        }
        channel.writeAndFlush(packet);
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        logger.info("Disconnected from server");
    }

    // Callback setters
    public void setLoginCallback(Consumer<LoginResponse> callback) { this.loginCallback = callback; }
    public void setRegisterCallback(Consumer<RegisterResponse> callback) { this.registerCallback = callback; }
    public void setMovementCallback(Consumer<MovementBroadcast> callback) { this.movementCallback = callback; }
    public void setPlayerLeftCallback(Consumer<PlayerLeftPacket> callback) { this.playerLeftCallback = callback; }
    public void setChatCallback(Consumer<ChatMessage> callback) { this.chatCallback = callback; }
    public void setChunkUpdateCallback(Consumer<ChunkUpdatePacket> callback) { this.chunkUpdateCallback = callback; }
    public void setMapSaveCallback(Consumer<MapSaveResponse> callback) { this.mapSaveCallback = callback; }
    public void setMapLoadCallback(Consumer<MapLoadResponse> callback) { this.mapLoadCallback = callback; }
    public void setChunkCallback(Consumer<Chunk> callback) { this.chunkCallback = callback; }
    public void setFriendListCallback(Consumer<FriendListResponse> callback) { this.friendListCallback = callback; }
    public void setFriendRequestCallback(Consumer<FriendRequestPacket> callback) { this.friendRequestCallback = callback; }
    public void setPrivateMessageCallback(Consumer<PrivateMessagePacket> callback) { this.privateMessageCallback = callback; }
    public void setPrivateMessageHistoryCallback(Consumer<PrivateMessageHistoryResponse> callback) { this.privateMessageHistoryCallback = callback; }
    public void setInventoryCallback(Consumer<InventoryUpdatePacket> callback) { this.inventoryCallback = callback; }
    public void setPickupResultCallback(Consumer<PickupResultPacket> callback) { this.pickupResultCallback = callback; }
    public void setItemSpawnCallback(Consumer<ItemSpawnPacket> callback) {
        this.itemSpawnCallback = callback;
        logger.info("ItemSpawnCallback set");
    }
    public void setItemDespawnCallback(Consumer<ItemDespawnPacket> callback) {
        this.itemDespawnCallback = callback;
        logger.info("ItemDespawnCallback set");
    }

    private class ClientHandler extends SimpleChannelInboundHandler<Object> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
            try {
                logger.info("Packet received: {}", msg.getClass().getSimpleName());

                if (msg instanceof LoginResponse && loginCallback != null) {
                    loginCallback.accept((LoginResponse) msg);
                } else if (msg instanceof RegisterResponse && registerCallback != null) {
                    registerCallback.accept((RegisterResponse) msg);
                } else if (msg instanceof MovementBroadcast && movementCallback != null) {
                    movementCallback.accept((MovementBroadcast) msg);
                } else if (msg instanceof PlayerLeftPacket && playerLeftCallback != null) {
                    playerLeftCallback.accept((PlayerLeftPacket) msg);
                } else if (msg instanceof ChatMessage && chatCallback != null) {
                    chatCallback.accept((ChatMessage) msg);
                } else if (msg instanceof ChunkUpdatePacket && chunkUpdateCallback != null) {
                    chunkUpdateCallback.accept((ChunkUpdatePacket) msg);
                } else if (msg instanceof MapSaveResponse && mapSaveCallback != null) {
                    logger.info("Map save response received: {}", ((MapSaveResponse) msg).message);
                    mapSaveCallback.accept((MapSaveResponse) msg);
                } else if (msg instanceof MapLoadResponse && mapLoadCallback != null) {
                    MapLoadResponse response = (MapLoadResponse) msg;
                    logger.info("Map load response: {} chunks", response.mapJson != null ? response.mapJson.getChunks().size() : 0);
                    mapLoadCallback.accept(response);
                } else if (msg instanceof FriendListResponse && friendListCallback != null) {
                    friendListCallback.accept((FriendListResponse) msg);
                } else if (msg instanceof FriendRequestPacket && friendRequestCallback != null) {
                    friendRequestCallback.accept((FriendRequestPacket) msg);
                } else if (msg instanceof PrivateMessagePacket && privateMessageCallback != null) {
                    privateMessageCallback.accept((PrivateMessagePacket) msg);
                } else if (msg instanceof PrivateMessageHistoryResponse && privateMessageHistoryCallback != null) {
                    privateMessageHistoryCallback.accept((PrivateMessageHistoryResponse) msg);
                } else if (msg instanceof Chunk && chunkCallback != null) {
                    chunkCallback.accept((Chunk) msg);
                    // ⭐ ADICIONAR ESTES CASOS PARA ITENS
                } else if (msg instanceof ItemSpawnPacket) {
                    logger.info("ItemSpawnPacket received, callback exists: {}", itemSpawnCallback != null);
                    if (itemSpawnCallback != null) {
                        itemSpawnCallback.accept((ItemSpawnPacket) msg);
                    } else {
                        logger.warn("ItemSpawnCallback is NULL!");
                    }
                } else if (msg instanceof ItemDespawnPacket) {
                    logger.info("ItemDespawnPacket received, callback exists: {}", itemDespawnCallback != null);
                    if (itemDespawnCallback != null) {
                        itemDespawnCallback.accept((ItemDespawnPacket) msg);
                    } else {
                        logger.warn("ItemDespawnCallback is NULL!");
                    }
                } else if (msg instanceof InventoryUpdatePacket && inventoryCallback != null) {
                    inventoryCallback.accept((InventoryUpdatePacket) msg);
                } else if (msg instanceof PickupResultPacket && pickupResultCallback != null) {
                    pickupResultCallback.accept((PickupResultPacket) msg);
                } else if (msg instanceof DropItemPacket) {
                    // Enviar para servidor (já está sendo enviado)
                }else {
                    logger.warn("Unhandled packet type: {}", msg.getClass().getSimpleName());
                }
            } catch (Exception e) {
                logger.error("Error processing packet", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            logger.error("Client handler exception: {}", cause.getMessage());
            ctx.close();
        }
    }
}