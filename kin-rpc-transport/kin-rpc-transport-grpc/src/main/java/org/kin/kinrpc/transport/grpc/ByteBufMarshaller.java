package org.kin.kinrpc.transport.grpc;

import io.grpc.MethodDescriptor;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCountUtil;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.transport.netty.AdaptiveOutputByteBufAllocator;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class ByteBufMarshaller implements MethodDescriptor.Marshaller<ByteBuf> {
    /** 单例 */
    public static final ByteBufMarshaller DEFAULT = new ByteBufMarshaller();

    /** byte buffer allocator */
    private final ByteBufAllocator allocator;
    /** 自适应分配bytebuf */
    private final AdaptiveOutputByteBufAllocator.Handle adaptiveHandle = AdaptiveOutputByteBufAllocator.DEFAULT.newHandle();

    private ByteBufMarshaller() {
        this(ByteBufAllocator.DEFAULT);
    }

    public ByteBufMarshaller(ByteBufAllocator allocator) {
        this.allocator = allocator;
    }

    @Override
    public InputStream stream(ByteBuf value) {
        return new ByteBufInputStream(value);
    }

    @Override
    public ByteBuf parse(InputStream stream) {
        ByteBuf byteBuf = adaptiveHandle.allocate(allocator);
        try {
            byteBuf.writeBytes(stream, stream.available());
            return byteBuf;
        } catch (IOException e) {
            ReferenceCountUtil.safeRelease(byteBuf);
            ExceptionUtils.throwExt(e);
        }
        return byteBuf;
    }
}