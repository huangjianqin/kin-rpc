package org.kin.kinrpc.transport.rpc.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.transport.rpc.domain.RPCResponse;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class RPCEncoder extends MessageToByteEncoder<RPCResponse> {
    private static final Logger log = LoggerFactory.getLogger("transport");

    private Serializer serializer;

    public RPCEncoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RPCResponse response, ByteBuf out) throws Exception {
        byte[] data = serializer.serialize(response);
        out.writeInt(data.length);
        out.writeBytes(data);

        InOutBoundStatisicService.instance().statisticResp(
                response.getServiceName() + "-" + response.getMethod(), data.length
        );
    }
}
