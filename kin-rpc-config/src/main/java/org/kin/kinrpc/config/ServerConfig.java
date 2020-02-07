package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.common.Constants;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Created by 健勤 on 2017/2/12.
 */
public class ServerConfig extends AbstractConfig {
    private static final int MAX_PORT = Short.MAX_VALUE - Short.MIN_VALUE;
    public static final ServerConfig DEFAULT = new ServerConfig(Constants.SERVER_DEFAULT_PORT);

    private String host;
    private int port;

    ServerConfig(int port) {
        this(NetUtils.getIp(), port);
    }

    ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
    }

    @Override
    void check() throws Exception{
        Preconditions.checkArgument(0 < port && port < MAX_PORT,
                "port must be greater than 0 and lower than " + MAX_PORT);
        try {
            InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            throw e;
        }

    }

    //getter
    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
