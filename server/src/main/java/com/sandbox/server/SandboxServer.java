package com.sandbox.server;

import com.common.sandbox.model.GroundItem;
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servidor principal Netty
 * Utiliza Java 21 com Virtual Threads para alta concorrência
 */
public class SandboxServer {
    private static final Logger logger = LoggerFactory.getLogger(SandboxServer.class);
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private final Map<String, GroundItem> localGroundItems = new ConcurrentHashMap<>();

    public SandboxServer(int port) {
        this.port = port;
    }

    public void start() throws Exception {
        try {
            // INICIALIZAR GERENCIADORES GLOBAIS ANTES DO SERVIDOR
            logger.info("========================================");
            logger.info("Inicializando gerenciadores do servidor...");
            logger.info("========================================");

            // Inicializar banco de dados
            DatabaseManager.getInstance();
            logger.info("✅ DatabaseManager inicializado");

            // Inicializar gerenciador de chunks
            ChunkManager.getInstance();
            logger.info("✅ ChunkManager inicializado");

            // Inicializar gerenciador de itens
            ItemManager itemManager = ItemManager.getInstance();
            logger.info("✅ ItemManager inicializado");

            // SPAWNAR ITENS MUNDIAIS (itens fixos que todos devem ver)
            logger.info("----------------------------------------");
            logger.info("Spawando itens mundiais...");
            itemManager.spawnWorldItems();
            logger.info("✅ Itens mundiais spawnados: {} itens", itemManager.getItemCount());
            logger.info("----------------------------------------");

            ProjectileManager.getInstance();

            // Verificar se os itens foram criados corretamente
            itemManager.printAllItems();

            logger.info("========================================");
            logger.info("Gerenciadores inicializados com sucesso!");
            logger.info("========================================");

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
            logger.info("========================================");
            logger.info("  Servidor iniciado na porta {}", port);
            logger.info("  Aguardando conexões dos clientes...");
            logger.info("========================================");

            // Iniciar threads de limpeza
            startCleanupTasks();

            future.channel().closeFuture().sync();

        } catch (Exception e) {
            logger.error("   Erro fatal ao iniciar o servidor: {}", e.getMessage(), e);
            throw e;
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

        // Salvar todas as posições antes de desligar
        GameWorld.getInstance().saveAllPlayersOnShutdown();

        // Desligar gerenciadores
        ItemManager.getInstance().shutdown();
        ChunkManager.getInstance().close();
        DatabaseManager.getInstance().close();
        RedisManager.getInstance().close();
        ProjectileManager.getInstance().shutdown();

        if (bossGroup != null) bossGroup.shutdownGracefully();
        if (workerGroup != null) workerGroup.shutdownGracefully();

        logger.info("  Servidor desligado completamente");
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8080;
        new SandboxServer(port).start();
    }
}