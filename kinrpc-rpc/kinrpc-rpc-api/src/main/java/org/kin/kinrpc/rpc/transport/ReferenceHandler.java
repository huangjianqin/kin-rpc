package org.kin.kinrpc.rpc.transport;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.domain.*;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.AbstractSession;
import org.kin.kinrpc.transport.ProtocolHandler;
import org.kin.kinrpc.transport.domain.AbstractProtocol;
import org.kin.kinrpc.transport.impl.Client;
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
    private static final Logger log = LoggerFactory.getLogger("transport");
    private Serializer serializer;
    private final RPCReference rpcReference;
    private Client client;
    //reference 端限流
    private RateLimiter rateLimiter = RateLimiter.create(org.kin.kinrpc.common.Constants.REFERENCE_REQUEST_THRESHOLD);

    private volatile Future heartbeatFuture;

    public ReferenceHandler(InetSocketAddress address, Serializer serializer, RPCReference rpcReference) {
        this(address, serializer, rpcReference, Constants.REFERENCE_DEFAULT_CONNECT_TIMEOUT);
    }

    public ReferenceHandler(InetSocketAddress address, Serializer serializer, RPCReference rpcReference, int connectTimeout) {
        super(address);
        this.serializer = serializer;
        this.rpcReference = rpcReference;

        this.client = new Client(address, new ReferenceProtocolTransfer(), this);
        this.client.setChannelInactiveListener(rpcReference);
        this.client.setChannelExceptionHandler(rpcReference);
        this.client.setTimeout(connectTimeout);
    }

    @Override
    public void connect() {
        while(!client.isStopped() && !client.isActive()){
            try {
                client.connect();
            } catch (Exception e) {
                log.error("", e);
            }
            if(client.isActive()){
               break;
            }

            try {
                //10s后重试
                Thread.sleep(10000);
            } catch (InterruptedException e) {

            }
        }

        if(!client.isStopped() && heartbeatFuture == null){
            heartbeatFuture = ThreadManager.DEFAULT.scheduleAtFixedRate(() -> {
                RPCHeartbeat heartbeat = new RPCHeartbeat(client.getLocalAddress(), "");
                client.request(heartbeat);
            }, 10, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void bind() throws Exception {
        client.bind();
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
        try {
            request.setCreateTime(System.currentTimeMillis());
            byte[] data = serializer.serialize(request);
            //限流
            rateLimiter.acquire();

            RPCRequestProtocol protocol = new RPCRequestProtocol(data);
            client.request(protocol);
        } catch (IOException e) {
            log.error("", e);
        }
    }

    public void setRate(double rate){
        rateLimiter.setRate(rate);
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
                    log.error("", e);
                    return;
                }

                rpcReference.handleResponse(rpcResponse);
            } catch (Exception e) {
                log.error("", e);
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
