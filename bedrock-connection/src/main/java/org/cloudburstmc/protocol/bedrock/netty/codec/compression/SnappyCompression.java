package org.cloudburstmc.protocol.bedrock.netty.codec.compression;

import io.airlift.compress.v3.snappy.SnappyJavaCompressor;
import io.airlift.compress.v3.snappy.SnappyJavaDecompressor;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.cloudburstmc.protocol.bedrock.data.CompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;

import java.lang.foreign.MemorySegment;
import java.util.zip.DataFormatException;

public class SnappyCompression implements BatchCompression {

    private static final int MAX_DECOMPRESSED_BYTES = Integer.getInteger("bedrock.maxDecompressedBytes", 1024 * 1024 * 10);

    // Snappy prefixes raw data with its uncompressed length as a varint of at most 5 bytes
    private static final int MAX_LENGTH_PREFIX_BYTES = 5;

    // The compressor keeps a hash table between calls, so each thread needs its own
    private static final ThreadLocal<SnappyJavaCompressor> COMPRESSOR = ThreadLocal.withInitial(SnappyJavaCompressor::new);
    private static final SnappyJavaDecompressor DECOMPRESSOR = new SnappyJavaDecompressor();

    @Override
    public ByteBuf encode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        SnappyJavaCompressor compressor = COMPRESSOR.get();
        ByteBuf output = ctx.alloc().directBuffer(compressor.maxCompressedLength(msg.readableBytes()));
        try {
            int compressed = compressor.compress(readableSegment(msg), writableSegment(output));
            output.writerIndex(output.writerIndex() + compressed);
            return output.retain();
        } finally {
            output.release();
        }
    }

    @Override
    public ByteBuf decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        byte[] lengthPrefix = new byte[Math.min(MAX_LENGTH_PREFIX_BYTES, msg.readableBytes())];
        msg.getBytes(msg.readerIndex(), lengthPrefix);
        int uncompressedLength = DECOMPRESSOR.getUncompressedLength(lengthPrefix, 0);
        if (uncompressedLength > MAX_DECOMPRESSED_BYTES) {
            throw new DataFormatException("Inflated data exceeds maximum size");
        }

        ByteBuf output = ctx.alloc().directBuffer(uncompressedLength);
        try {
            int decompressed = DECOMPRESSOR.decompress(readableSegment(msg), writableSegment(output));
            output.writerIndex(output.writerIndex() + decompressed);
            return output.retain();
        } finally {
            output.release();
        }
    }

    private static MemorySegment readableSegment(ByteBuf buf) {
        return MemorySegment.ofBuffer(buf.nioBuffer(buf.readerIndex(), buf.readableBytes()));
    }

    private static MemorySegment writableSegment(ByteBuf buf) {
        // A single component buffer's NIO view shares its memory, so writes land in buf
        if (buf.nioBufferCount() != 1) {
            throw new IllegalStateException("Unsupported ByteBuf " + buf.getClass().getSimpleName());
        }
        return MemorySegment.ofBuffer(buf.nioBuffer(buf.writerIndex(), buf.writableBytes()));
    }

    @Override
    public CompressionAlgorithm getAlgorithm() {
        return PacketCompressionAlgorithm.SNAPPY;
    }

    @Override
    public void setLevel(int level) {
    }

    @Override
    public int getLevel() {
        return -1;
    }
}
