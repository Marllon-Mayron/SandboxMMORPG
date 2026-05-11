package com.sandbox.server;

import com.common.sandbox.network.KryoRegistry;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class KryoCodec extends ByteToMessageCodec<Object> {
    private static final Logger logger = LoggerFactory.getLogger(KryoCodec.class);
    private final Kryo kryo;

    public KryoCodec() {
        this.kryo = new Kryo();
        KryoRegistry.registerClasses(kryo);  // ← Usa o registro unificado
        logger.info("KryoCodec initialized with unified registry");
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        try {
            ByteBufOutputStream bos = new ByteBufOutputStream(out);
            Output output = new Output(bos);
            kryo.writeClassAndObject(output, msg);
            output.flush();
            output.close();
        } catch (Exception e) {
            logger.error("Encode error: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < 4) {
            return;
        }

        try {
            in.markReaderIndex();

            ByteBufInputStream bis = new ByteBufInputStream(in);
            Input input = new Input(bis);

            Object obj = kryo.readClassAndObject(input);

            if (obj != null) {
                logger.debug("Decoded object: {}", obj.getClass().getSimpleName());
                out.add(obj);
            }

            input.close();

        } catch (Exception e) {
            in.resetReaderIndex();
            logger.error("Decode error: {}", e.getMessage());
            int skipBytes = Math.min(in.readableBytes(), 1024);
            in.skipBytes(skipBytes);
        }
    }
}