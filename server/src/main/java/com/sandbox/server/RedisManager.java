package com.sandbox.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gerenciador de Redis (Cache)
 * Por enquanto é um placeholder - Redis é opcional
 */
public class RedisManager {
    private static final Logger logger = LoggerFactory.getLogger(RedisManager.class);
    private static RedisManager instance;
    private boolean available = false;

    private RedisManager() {
        // Tentar conectar ao Redis se disponível
        try {
            // Por enquanto, não conectar realmente
            logger.info("Redis manager inicializado (modo simulação)");
            available = false;
        } catch (Exception e) {
            logger.warn("Redis não disponível, continuando sem cache");
            available = false;
        }
    }

    public static synchronized RedisManager getInstance() {
        if (instance == null) {
            instance = new RedisManager();
        }
        return instance;
    }

    public void close() {
        logger.info("Redis manager fechado");
    }

    public boolean isAvailable() {
        return available;
    }
}