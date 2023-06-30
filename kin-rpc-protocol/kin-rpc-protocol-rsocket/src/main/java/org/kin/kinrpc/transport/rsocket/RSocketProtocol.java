package org.kin.kinrpc.transport.rsocket;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;

import javax.annotation.Nullable;

/**
 * @author huangjianqin
 * @date 2021/1/30
 */
@Extension("rsocket")
public class RSocketProtocol extends AbstractProtocol {

    @Override
    protected RemotingServer createServer(ServerConfig serverConfig, @Nullable ManagedExecutor executor) {
        return new RSocketServer(serverConfig.getHost(), serverConfig.getPort(), executor, serverConfig.getSsl());
    }

    @Override
    protected RemotingClient createClient(ServiceInstance instance, SslConfig sslConfig) {
        return new RSocketClient(instance.host(), instance.port(), sslConfig);
    }
}
