package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.protocol.Protocol;
import org.kin.kinrpc.protocol.Protocols;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class JvmServiceBootstrap<T> extends ServiceBootstrap<T> {
    /** rpc service */
    private volatile RpcService<T> rpcService;
    /** service exporter */
    private volatile Exporter<T> exporter;

    public JvmServiceBootstrap(ServiceConfig<T> config) {
        super(config);
    }

    @Override
    protected void doExport() {
        if (Objects.isNull(exporter)) {
            return;
        }

        //创建rpc service
        rpcService = new RpcService<>(config);

        Protocol protocol = Protocols.getByName(ProtocolType.JVM.getName());
        exporter = protocol.export(rpcService, null);
    }

    @Override
    protected void doUnExport() {
        //service destroy
        rpcService.destroy();
        //shutdown server
        exporter.unExport();

        //help gc
        rpcService = null;
        exporter = null;
    }
}
