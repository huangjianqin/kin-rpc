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
import org.kin.kinrpc.transport.netty.*;
import org.kin.kinrpc.transport.netty.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceHandler extends AbstractConnection implements ProtocolHandler {
    private static final Logger log = LoggerFactory.getLogger(ReferenceHandler.class);
    private Serializer serializer;
    private final RPCReference rpcReference;
    private Client client;

    private volatile Future heartbeatFuture;

    public ReferenceHandler(InetSocketAddress address, Serializer serializer, RPCReference rpcReference) {
        super(address);
        this.serializer = serializer;
        this.rpcReference = rpcReference;

        this.client = new Client(address);
    }

    @Override
    public void connect(TransportOption transportOption) {
        if(!client.isStopped() && !client.isActive()){
            try {
                client.connect(transportOption);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if(!client.isActive()){
                /** n秒后重连 */
                RPCThreadPool.THREADS.schedule(() -> connect(transportOption), 10, TimeUnit.SECONDS);
            }
        }

        if(!client.isStopped() && heartbeatFuture == null){
            heartbeatFuture = RPCThreadPool.THREADS.scheduleAtFixedRate(() -> {
                RPCHeartbeat heartbeat = ProtocolFactory.createProtocol(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID, client.getLocalAddress(), "");
                client.request(heartbeat);
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void bind(TransportOption transportOption) throws Exception {
        client.bind(transportOption);
    }

    @Override
    public void close() {
        client.close();
        heartbeatFuture.cancel(false);
    }

    @Override
    public boolean isActive() {
        return client.isActive();
    }

    public void request(RPCRequest request){
        if(isActive()){
            try {
                request.setCreateTime(System.currentTimeMillis());
                byte[] data = serializer.serialize(request);

                RPCRequestProtocol protocol = ProtocolFactory.createProtocol(RPCConstants.RPC_REQUEST_PROTOCOL_ID, data);
                client.request(protocol);
            } catch (IOException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleProtocol(AbstractSession session, AbstractProtocol protocol) {
        if(protocol == null){
            return;
        }
        if(protocol instanceof RPCResponseProtocol){
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

                rpcReference.handleResponse(rpcResponse);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        else if(protocol instanceof RPCHeartbeat){
            RPCHeartbeat heartbeat = (RPCHeartbeat) protocol;
            log.info("server heartbeat ip:{}, content:{}", heartbeat.getIp(), heartbeat.getContent());
        }
        else{
            log.error("unknown protocol >>>> {}", protocol);
        }
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

        return client.equals(that.client);
    }

    @Override
    public int hashCode() {
        return client.hashCode();
    }
}
