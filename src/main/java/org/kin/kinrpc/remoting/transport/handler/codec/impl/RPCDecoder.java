package org.kin.kinrpc.remoting.transport.handler.codec.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.serializer.Serializer;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class RPCDecoder extends ByteToMessageDecoder {
    private Serializer serializer;

    public RPCDecoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        //可读字节数少于数据帧长度标识 => 不合法的数据帧
        if (in.readableBytes() < Constants.FRAMELENGTH_FIELD_LENGTH) {
            return;
        }

        in.markReaderIndex();
        int dataLength = in.readInt();
        if (in.readableBytes() < dataLength) {
            in.resetReaderIndex();
            return;
        }

        byte[] data = new byte[dataLength];
        in.readBytes(data);
        out.add(serializer.deserialize(data));
    }
}
