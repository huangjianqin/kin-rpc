package org.kin.kinrpc.transport.kinrpc;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.Transport;

import javax.annotation.Nullable;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
@Extension("kinrpc")
public class KinRpcTransport implements Transport {
    @Override
    public RemotingServer createServer(String host, int port, @Nullable ManagedExecutor executor, @Nullable SslConfig sslConfig) {
        return new KinRpcServer(host, port, executor, sslConfig);
    }

    @Override
    public RemotingClient createClient(String host, int port, @Nullable SslConfig sslConfig) {
        return new KinRpcClient(host, port, sslConfig);
    }
}
