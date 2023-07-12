package org.kin.kinrpc.transport;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;

import javax.annotation.Nullable;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
@SPI(alias = "transport")
public interface Transport {
    /**
     * create remoting server
     *
     * @param host      listen host
     * @param port      listen port
     * @param executor  command handler executor
     * @param sslConfig ssl config
     * @return remoting server instance
     */
    RemotingServer createServer(String host,
                                int port,
                                @Nullable ManagedExecutor executor,
                                @Nullable SslConfig sslConfig);

    /**
     * create remoting client
     *
     * @param host      remoting host
     * @param port      remoting port
     * @param sslConfig ssl config
     * @return remoting client instance
     */
    RemotingClient createClient(String host,
                                int port,
                                @Nullable SslConfig sslConfig);
}
