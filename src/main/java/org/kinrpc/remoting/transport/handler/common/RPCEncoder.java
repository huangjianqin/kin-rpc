package org.kinrpc.remoting.transport.handler.common;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.apache.log4j.Logger;
import org.kinrpc.rpc.protocol.serializer.Serializer;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class RPCEncoder extends MessageToByteEncoder {
    private static final Logger log = Logger.getLogger(RPCEncoder.class);

    private Serializer serializer;

    public RPCEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, Object t, ByteBuf byteBuf) throws Exception {
        byte[] data = serializer.serialize(t);
        log.info("data frame's length=" + data.length);
        byteBuf.writeInt(data.length);
        byteBuf.writeBytes(data);
    }
}
