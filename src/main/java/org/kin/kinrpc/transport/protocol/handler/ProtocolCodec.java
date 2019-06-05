package org.kin.kinrpc.transport.protocol.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import org.kin.kinrpc.transport.protocol.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.protocol.Session;
import org.kin.kinrpc.transport.protocol.ProtocolConstants;
import org.kin.kinrpc.transport.protocol.domain.ProtocolByteBuf;
import org.kin.kinrpc.transport.protocol.domain.Request;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by huangjianqin on 2019/5/29.
 */
public class ProtocolCodec extends MessageToMessageCodec<ByteBuf, ProtocolByteBuf> {
    private static final Logger log = LoggerFactory.getLogger("transport");
    private final Bytes2ProtocolTransfer transfer;
    //true = server, false = client
    private boolean serverElseClient;

    public ProtocolCodec(Bytes2ProtocolTransfer transfer, boolean serverElseClient) {
        this.transfer = transfer;
        this.serverElseClient = serverElseClient;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ProtocolByteBuf protocolByteBuf, List<Object> out) throws Exception {
        if (serverElseClient) {
            //server send response
            ByteBuf outByteBuf = ctx.alloc().buffer();
            ByteBuf byteBuf = protocolByteBuf.getByteBuf();
            outByteBuf.writeInt(byteBuf.readableBytes() + 4);
            outByteBuf.writeInt(getRespSN(ctx.channel()));
            outByteBuf.writeBytes(byteBuf, 0, byteBuf.readableBytes());
            out.add(outByteBuf);

            InOutBoundStatisicService.instance().statisticResp(protocolByteBuf.getProtocolId() + "", protocolByteBuf.getSize());
        } else {
            //client send request
            ByteBuf outByteBuf = ctx.alloc().buffer();
            ByteBuf byteBuf = protocolByteBuf.getByteBuf();
            outByteBuf.writeInt(byteBuf.readableBytes());
            outByteBuf.writeBytes(byteBuf, 0, byteBuf.readableBytes());
            out.add(outByteBuf);

            InOutBoundStatisicService.instance().statisticReq(protocolByteBuf.getProtocolId() + "", protocolByteBuf.getSize());
        }
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //对于server还是client, 直接解析成AbstractProtocol
        if (serverElseClient) {
            Request byteBufRequest = new ProtocolByteBuf(in);
            out.add(transfer.transfer(byteBufRequest));

            //server receive request
            InOutBoundStatisicService.instance()
                    .statisticReq(byteBufRequest.getProtocolId() + "", byteBufRequest.getContentSize());
        } else {
            //client 需要解析respSN
            Request byteBufRequest = new ProtocolByteBuf(in, true);
            out.add(transfer.transfer(byteBufRequest));

            //client receive response
            InOutBoundStatisicService.instance()
                    .statisticResp(byteBufRequest.getProtocolId() + "", byteBufRequest.getContentSize());
        }
    }

    private int getRespSN(Channel channel) {
        Session session = ProtocolConstants.session(channel);
        if (session != null) {
            return session.getRespSN();
        }
        return -1;
    }
}
