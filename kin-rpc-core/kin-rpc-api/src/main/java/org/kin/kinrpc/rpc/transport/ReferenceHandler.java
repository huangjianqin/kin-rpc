package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.RPCThreadPool;
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
import java.util.Objects;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceHandler implements ProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(ReferenceHandler.class);
    private final Serializer serializer;
    private final RPCReference rpcReference;
    private final InetSocketAddress address;

    private volatile Client client;
    private volatile Future heartbeatFuture;

    public ReferenceHandler(InetSocketAddress address, Serializer serializer, RPCReference rpcReference) {
        this.address = address;
        this.serializer = serializer;
        this.rpcReference = rpcReference;
    }

    public void connect(ClientTransportOption transportOption) {
        if (isActive()) {
            return;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (client == null) {
            try {
                client = transportOption.tcp(address);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (client == null || (!client.isActive())) {
                /** n秒后重连 */
                RPCThreadPool.THREADS.schedule(() -> connect(transportOption), 10, TimeUnit.SECONDS);
            }
        }

        if (heartbeatFuture == null) {
            heartbeatFuture = RPCThreadPool.THREADS.scheduleAtFixedRate(() -> {
                if (client != null) {
                    try {
                        RPCHeartbeat heartbeat = ProtocolFactory.createProtocol(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID, client.getLocalAddress(), "");
                        client.request(heartbeat);
                    } catch (Exception e) {
                        //屏蔽异常
                    }
                }
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    public void close() {
        if (isActive()) {
            client.close();
            heartbeatFuture.cancel(false);
        }
    }

    public boolean isActive() {
        return client != null && client.isActive();
    }

    public void request(RPCRequest request) {
        if (isActive()) {
            try {
                request.setCreateTime(System.currentTimeMillis());
                byte[] data = serializer.serialize(request);

                RPCRequestProtocol protocol = ProtocolFactory.createProtocol(RPCConstants.RPC_REQUEST_PROTOCOL_ID, data);
                client.request(protocol);

                InOutBoundStatisicService.instance().statisticReq(
                        request.getServiceName() + "-" + request.getMethod(), data.length
                );
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        if (!isActive()) {
            return;
        }
        if (protocol == null) {
            return;
        }
        if (protocol instanceof RPCResponseProtocol) {
            RPCResponseProtocol responseProtocol = (RPCResponseProtocol) protocol;
            try {
                RPCResponse rpcResponse;
                try {
                    rpcResponse = serializer.deserialize(responseProtocol.getRespContent(), RPCResponse.class);
                    rpcResponse.setEventTime(System.currentTimeMillis());
                } catch (IOException | ClassNotFoundException e) {
                    log.error(e.getMessage(), e);
                    return;
                }

                InOutBoundStatisicService.instance().statisticResp(
                        rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), responseProtocol.getRespContent().length
                );

                rpcReference.handleResponse(rpcResponse);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        } else if (protocol instanceof RPCHeartbeat) {
            RPCHeartbeat heartbeat = (RPCHeartbeat) protocol;
            log.info("server heartbeat ip:{}, content:{}", heartbeat.getIp(), heartbeat.getContent());
        } else {
            log.error("unknown protocol >>>> {}", protocol);
        }
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getAddressStr() {
        return this.address.getHostName() + ":" + this.address.getPort();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ReferenceHandler that = (ReferenceHandler) o;
        return Objects.equals(client, that.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(client);
    }
}
