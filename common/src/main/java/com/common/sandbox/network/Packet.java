package com.common.sandbox.network;

/**
 * Classe base para todos os pacotes da rede
 * Usaremos Kryo para serialização automática
 */
public abstract class Packet {
    // Timestamp para controle de latência
    public long timestamp;

    public Packet() {
        this.timestamp = System.currentTimeMillis();
    }
}