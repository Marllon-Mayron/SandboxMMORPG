package com.sandbox.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Servidor principal Netty
 * Utiliza Java 21 com Virtual Threads para alta concorrência
 */
public class SandboxServer {
    private static final Logger logger = LoggerFactory.getLogger(SandboxServer.class);
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;

    public SandboxServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        try {
            // Usando Virtual Threads do Java 21 para workers
            bossGroup = new NioEventLoopGroup(1); // Aceita conexões
            workerGroup = new NioEventLoopGroup(Runtime.getRuntime().availableProcessors()); // Processa dados

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // Timeout de leitura (30 segundos sem dados = desconecta)
                            pipeline.addLast(new ReadTimeoutHandler(30));

                            // Frame decoder para mensagens delimitadas por tamanho
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));

                            // Serialização Kryo
                            pipeline.addLast(new KryoCodec());

                            // Handler de mensagens
                            pipeline.addLast(new GameServerHandler());
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true);

            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("Servidor iniciado na porta {}", port);

            // Iniciar threads de limpeza
            startCleanupTasks();

            future.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }

    private void startCleanupTasks() {
        // Thread para limpar mensagens de chat antigas (a cada 300 segundos)
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(300_000); // 5 minutos
                    GameWorld.getInstance().cleanOldChatMessages();
                    logger.info("Chat history cleaned");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });

        // Thread para verificar jogadores inativos
        Thread.startVirtualThread(() -> {
            while (true) {
                try {
                    Thread.sleep(60_000); // A cada minuto
                    GameWorld.getInstance().checkInactivePlayers();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
    }

    public void shutdown() {
        logger.info("Desligando servidor...");
        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        DatabaseManager.getInstance().close();
        RedisManager.getInstance().close();
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new SandboxServer(port).start();
    }
}