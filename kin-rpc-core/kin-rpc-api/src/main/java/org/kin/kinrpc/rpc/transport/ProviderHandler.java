package org.kin.kinrpc.rpc.transport;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.channel.Channel;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.RPCProvider;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.transport.protocol.RPCHeartbeat;
import org.kin.kinrpc.rpc.transport.protocol.RPCRequestProtocol;
import org.kin.kinrpc.rpc.transport.protocol.RPCResponseProtocol;
import org.kin.kinrpc.transport.netty.*;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderHandler extends AbstractConnection implements ProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(ProviderHandler.class);
    private final RPCProvider rpcProvider;
    private Serializer serializer;
    private Server server;
    private RateLimiter rateLimiter = RateLimiter.create(Constants.SERVER_REQUEST_THRESHOLD);

    public ProviderHandler(InetSocketAddress address,
                           RPCProvider rpcProvider,
                           Serializer serializer) {
        super(address);
        this.rpcProvider = rpcProvider;
        this.serializer = serializer;

        this.server = new Server(address);
    }

    @Override
    public void connect(TransportOption transportOption) {
        server.connect(transportOption);
    }

    @Override
    public void bind(TransportOption transportOption) throws Exception {
        server.bind(transportOption);
    }

    @Override
    public void close() {
        server.close();
    }

    @Override
    public boolean isActive() {
        return server.isActive();
    }

    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        if(protocol == null){
            return;
        }
        if(protocol instanceof RPCRequestProtocol){
            try {
                RPCRequestProtocol requestProtocol = (RPCRequestProtocol) protocol;
                byte[] data = requestProtocol.getReqContent();

                RPCRequest rpcRequest = null;
                try {
                    rpcRequest = serializer.deserialize(data, RPCRequest.class);

                    InOutBoundStatisicService.instance().statisticReq(
                            rpcRequest.getServiceName() + "-" + rpcRequest.getServiceName(), data.length
                    );

                    //限流
                    if(!rateLimiter.tryAcquire(data.length)){
                        RPCResponse rpcResponse = RPCResponse.respWithError(rpcRequest, "server rate limited, just reject");
                        session.getChannel().writeAndFlush(rpcResponse);

                        return;
                    }

                    rpcRequest.setChannel(session.getChannel());
                    rpcRequest.setEventTime(System.currentTimeMillis());
                } catch (IOException | ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                    RPCResponse rpcResponse = RPCResponse.respWithError(rpcRequest, e.getMessage());
                    session.getChannel().writeAndFlush(rpcResponse);
                    return;
                }

                //简单地添加到任务队列交由上层的线程池去完成服务调用
                rpcProvider.handleRequest(rpcRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        else if(protocol instanceof RPCHeartbeat){
            RPCHeartbeat heartbeat = (RPCHeartbeat) protocol;
            log.info("client heartbeat ip:{}, content:{}", heartbeat.getIp(), heartbeat.getContent());
            RPCHeartbeat heartbeatResp = ProtocolFactory.createProtocol(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID, getAddress(), "");
            session.getChannel().writeAndFlush(heartbeatResp.write());
        }
        else{
            log.error("unknown protocol >>>> {}", protocol);
        }
    }

    public void resp(Channel channel, RPCResponse rpcResponse) {
        byte[] data;
        try {
            data = serializer.serialize(rpcResponse);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
            rpcResponse.setState(RPCResponse.State.ERROR, e.getMessage());
            rpcResponse.setResult(null);
            try {
                data = serializer.serialize(rpcResponse);
            } catch (IOException e1) {
                log.error(e1.getMessage(), e1);
                return;
            }
        }

        RPCResponseProtocol rpcResponseProtocol = ProtocolFactory.createProtocol(RPCConstants.RPC_RESPONSE_PROTOCOL_ID, data);
        channel.writeAndFlush(rpcResponseProtocol.write());
    }
}
