package org.kin.kinrpc.transport.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * Created by huangjianqin on 2019/5/29.
 * 主要是校验协议头
 */
public class BaseFrameCodec extends ByteToMessageCodec<ByteBuf> {
    private static final Logger log = LoggerFactory.getLogger(BaseFrameCodec.class);
    private static final byte[] FRAME_MAGIC = "kin-rpc".getBytes();
    private final int FRAME_SNO_SIZE = 4;
    private final int FRAME_BODY_SIZE = 4;
    private final int MAX_BODY_SIZE;
    //true = in, false = out
    private final boolean serverElseClient;

    private final int FRAME_BASE_LENGTH;


    public BaseFrameCodec(int maxBodySize, boolean serverElseClient) {
        this.MAX_BODY_SIZE = maxBodySize;
        this.serverElseClient = serverElseClient;
        this.FRAME_BASE_LENGTH = FRAME_SNO_SIZE + FRAME_MAGIC.length + FRAME_BODY_SIZE;
    }

    public static BaseFrameCodec clientRPCFrameCodec() {
        return new BaseFrameCodec(1024000, false);
    }

    public static BaseFrameCodec serverRPCFrameCodec() {
        return new BaseFrameCodec(1024000, true);
    }

    public static BaseFrameCodec clientFrameCodec() {
        return new BaseFrameCodec(1024000, false);
    }

    public static BaseFrameCodec serverFrameCodec() {
        return new BaseFrameCodec(1024000, true);
    }

    private static boolean isMagicRight(byte[] magic) {
        return Arrays.equals(magic, FRAME_MAGIC);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        if (!serverElseClient) {
            out.writeBytes(FRAME_MAGIC);
            //TODO sno
            out.writeInt(0);
        }
        int bodySize = in.readableBytes();
        out.writeInt(bodySize);
        out.writeBytes(in, bodySize);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (serverElseClient) {
            if (in.readableBytes() >= FRAME_BASE_LENGTH) {
                byte[] magic = new byte[FRAME_MAGIC.length];
                in.readBytes(magic);

                //校验魔数
                if (!isMagicRight(magic)) {
                    String hexDump = ByteBufUtil.hexDump(in);
                    in.skipBytes(in.readableBytes());
                    throw new CorruptedFrameException(String.format("FrameHeaderError: magic=%s, HexDump=%s", Arrays.toString(magic), hexDump));
                }

                int sno = in.readInt();
                int bodySize = in.readInt();

                if (bodySize > MAX_BODY_SIZE) {
                    throw new IllegalStateException(String.format("BodySize[%s] too large!", bodySize));
                }

                int bodyReadableSize = in.readableBytes();
                if (bodyReadableSize != bodySize) {
                    throw new IllegalStateException(String.format("BodyReadableSize[%s] != BodySize[%s]!", bodyReadableSize, bodySize));
                }

                ByteBuf frameBuf = ctx.alloc().heapBuffer(bodySize);
                frameBuf.writeBytes(in, bodySize);
                out.add(frameBuf);
            }
        }
        else{
            int bodySize = in.readInt();
            ByteBuf frameBuf = ctx.alloc().heapBuffer(bodySize);
            frameBuf.writeBytes(in, bodySize);
            out.add(frameBuf);
        }
    }
}
