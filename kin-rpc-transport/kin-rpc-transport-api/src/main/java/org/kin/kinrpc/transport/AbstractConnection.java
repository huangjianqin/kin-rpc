package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.domain.TransportOption;

import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/10.
 */
public abstract class AbstractConnection<T extends TransportOption<?>> {
    protected final InetSocketAddress address;

    public AbstractConnection(InetSocketAddress address) {
        this.address = address;
    }

    public abstract void connect(T transportOption);

    public abstract void bind(T transportOption) throws Exception;

    public abstract void close();

    public String getAddress() {
        return address.getHostName() + ":" + address.getPort();
    }

    public abstract boolean isActive();
}
