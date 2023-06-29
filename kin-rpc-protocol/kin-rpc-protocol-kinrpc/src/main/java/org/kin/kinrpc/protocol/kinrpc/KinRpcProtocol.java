package org.kin.kinrpc.protocol.kinrpc;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingServer;
import org.kin.kinrpc.transport.kinrpc.KinRpcClient;
import org.kin.kinrpc.transport.kinrpc.KinRpcServer;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
@Extension("kinrpc")
public final class KinRpcProtocol extends AbstractProtocol {
    @Override
    protected RemotingServer createServer(ServerConfig serverConfig) {
        return new KinRpcServer(serverConfig.getHost(), serverConfig.getPort(), serverConfig.getSsl());
    }

    @Override
    protected RemotingClient createClient(ServiceInstance instance, SslConfig sslConfig) {
        return new KinRpcClient(instance.host(), instance.port(), sslConfig);
    }
}
