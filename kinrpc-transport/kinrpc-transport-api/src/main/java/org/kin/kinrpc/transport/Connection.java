package org.kin.kinrpc.transport;

import java.net.InetSocketAddress;

/**
 * Created by 健勤 on 2017/2/10.
 */
public abstract class Connection {
    protected final InetSocketAddress address;

    public Connection(InetSocketAddress address) {
        this.address = address;
    }

    public abstract void connect();

    public abstract void bind() throws Exception;

    public abstract void close();

    public String getAddress() {
        return address.getHostName() + address.getPort();
    }

    public abstract boolean isActive();
}
