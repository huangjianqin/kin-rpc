package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;

/**
 * Created by 健勤 on 2017/2/12.
 */
class ServerConfig extends AbstractConfig{
    private static final int MAX_PORT = Short.MAX_VALUE - Short.MIN_VALUE;
    private int port;

    public ServerConfig(int port) {
        this.port = port;
    }

    @Override
    public void check() {
        Preconditions.checkArgument(port < 0 && port > MAX_PORT,
                "port must be greater than 0 and lower than " + MAX_PORT);

    }

    //setter && getter
    public int getPort() {
        return port;
    }
}
