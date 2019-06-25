package org.kin.kinrpc.rpc.transport;

import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.transport.AbstractConnection;
import org.kin.kinrpc.transport.protocol.AbstractSession;
import org.kin.kinrpc.transport.protocol.Client;
import org.kin.kinrpc.transport.protocol.ProtocolHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceHandler extends AbstractConnection implements ProtocolHandler<RPCResponseProtocol> {
    private static final Logger log = LoggerFactory.getLogger("transport");
    private Serializer serializer;
    private final RPCReference rpcReference;
    private Client client;

    public ReferenceHandler(InetSocketAddress address, RPCReference rpcReference) {
        this(address, SerializerType.HESSION.newInstance(), rpcReference);
    }

    public ReferenceHandler(InetSocketAddress address, Serializer serializer, RPCReference rpcReference) {
        super(address);
        this.serializer = serializer;
        this.rpcReference = rpcReference;

        this.client = new Client(address, new RPCResponseProtocolTransfer(), this);
        this.client.setTimeout(500);
    }

    @Override
    public void connect() {
        client.connect();
    }

    @Override
    public void bind() throws Exception {
        client.bind();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public boolean isActive() {
        return client.isActive();
    }

    public void request(RPCRequest request) throws Exception {
        try {
            byte[] data = serializer.serialize(request);
            RPCRequestProtocol protocol = new RPCRequestProtocol(data);
            client.request(protocol);
        } catch (IOException e) {
            log.error("", e);
            throw e;
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

    @Override
    public void handleProtocol(AbstractSession session, RPCResponseProtocol protocol) {
        try {
            RPCResponse rpcResponse;
            try {
                rpcResponse = serializer.deserialize(protocol.getRespContent());
            } catch (IOException | ClassNotFoundException e) {
                log.error("", e);
                return;
            }

            rpcReference.handleResponse(rpcResponse);
        } catch (Exception e) {
            log.error("", e);
        }
    }
}
