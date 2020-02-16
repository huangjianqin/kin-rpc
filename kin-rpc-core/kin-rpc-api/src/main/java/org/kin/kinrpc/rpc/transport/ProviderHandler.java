package org.kin.kinrpc.rpc.transport;

import com.google.common.util.concurrent.RateLimiter;
import io.netty.channel.Channel;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.RPCProvider;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.transport.protocol.RPCHeartbeat;
import org.kin.kinrpc.rpc.transport.protocol.RPCRequestProtocol;
import org.kin.kinrpc.rpc.transport.protocol.RPCResponseProtocol;
import org.kin.transport.netty.core.*;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/10.
 */
public class ProviderHandler implements ProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(ProviderHandler.class);
    private final InetSocketAddress address;
    private final RPCProvider rpcProvider;
    private Serializer serializer;
    private Server server;
    private RateLimiter rateLimiter = RateLimiter.create(Constants.SERVER_REQUEST_THRESHOLD);

    public ProviderHandler(InetSocketAddress address,
                           RPCProvider rpcProvider,
                           Serializer serializer) {
        this.address = address;
        this.rpcProvider = rpcProvider;
        this.serializer = serializer;
    }

    public void bind(ServerTransportOption transportOption) throws Exception {
        if (server != null) {
            server.close();
        }
        server = transportOption.tcp(address);
    }

    public void close() {
        if (server != null) {
            server.close();
        }
    }

    public boolean isActive() {
        return server != null && server.isActive();
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getAddressStr() {
        return this.address.getHostName() + ":" + this.address.getPort();
    }

    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        if (protocol == null) {
            return;
        }
        if (protocol instanceof RPCRequestProtocol) {
            try {
                RPCRequestProtocol requestProtocol = (RPCRequestProtocol) protocol;
                byte[] data = requestProtocol.getReqContent();

                RPCRequest rpcRequest = null;
                try {
                    rpcRequest = serializer.deserialize(data, RPCRequest.class);

                    InOutBoundStatisicService.instance().statisticReq(
                            rpcRequest.getServiceName() + "-" + rpcRequest.getMethod(), data.length
                    );

                    //限流
                    if (!rateLimiter.tryAcquire(data.length)) {
                        RPCResponse rpcResponse = RPCResponse.respWithError(rpcRequest, "server rate limited, just reject");
                        session.getChannel().writeAndFlush(rpcResponse);

                        return;
                    }

                    rpcRequest.setChannel(session.getChannel());
                    rpcRequest.setEventTime(System.currentTimeMillis());
                } catch (IOException | ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                    RPCResponse rpcResponse = RPCResponse.respWithError(rpcRequest, ExceptionUtils.getExceptionDesc(e));
                    session.getChannel().writeAndFlush(rpcResponse);
                    return;
                }

                //简单地添加到任务队列交由上层的线程池去完成服务调用
                rpcProvider.handleRequest(rpcRequest);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if (protocol instanceof RPCHeartbeat) {
            RPCHeartbeat heartbeat = (RPCHeartbeat) protocol;
            log.info("provider({}) receive heartbeat ip:{}, content:{}", address, heartbeat.getIp(), heartbeat.getContent());
            RPCHeartbeat heartbeatResp = ProtocolFactory.createProtocol(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID, getAddressStr(), "");
            session.getChannel().writeAndFlush(heartbeatResp.write());
        } else {
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

        InOutBoundStatisicService.instance().statisticResp(
                rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), data.length
        );
    }
}
