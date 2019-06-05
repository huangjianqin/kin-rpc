package org.kin.kinrpc.transport.rpc.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import org.kin.kinrpc.serializer.Serializer;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class RPCDecoder extends ByteToMessageDecoder {
    private static final Logger log = LoggerFactory.getLogger("transport");
    private Serializer serializer;

    public RPCDecoder(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    protected void decode(ChannelHandlerContext channelHandlerContext, ByteBuf in, List<Object> out) throws Exception {
        byte[] data = new byte[in.readableBytes()];
        in.readBytes(data);

        RPCRequest rpcRequest = serializer.deserialize(data);
        out.add(rpcRequest);

        InOutBoundStatisicService.instance().statisticReq(
                rpcRequest.getServiceName() + "-" + rpcRequest.getServiceName(), data.length
        );
    }
}
