package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.KinRpcRuntimeContext;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.protocol.Protocol;
import org.kin.kinrpc.protocol.Protocols;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.registry.RegistryHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * default boostrap service
 *
 * @author huangjianqin
 * @date 2023/6/30
 */
public class DefaultServiceBootstrap<T> extends ServiceBootstrap<T> {
    /** rpc service */
    private volatile RpcService<T> rpcService;
    /** service exporter */
    private volatile Exporter<T> exporter;

    public DefaultServiceBootstrap(ServiceConfig<T> config) {
        super(config);
    }

    @Override
    protected void doExport() {
        //创建rpc service
        rpcService = new RpcService<>(config);

        List<Exporter<T>> exporters = new ArrayList<>();
        //创建server, 并注册监听接受处理该service的请求
        for (ServerConfig serverConfig : config.getServers()) {
            String protocolName = serverConfig.getProtocol();
            Protocol protocol = Protocols.getByName(protocolName);

            exporters.add(protocol.export(rpcService, serverConfig));

            //暴露内部服务
            ApplicationGuardian guardian = ApplicationGuardian.instance();
            guardian.exportInternalService(serverConfig);
        }

        //获取注册中心client, 并发布服务
        for (RegistryConfig registryConfig : config.getRegistries()) {
            Registry registry = RegistryHelper.createRegistryIfAbsent(registryConfig);
            registry.register(config);
        }

        exporter = new CompositeExporter<>(exporters);
        KinRpcRuntimeContext.cacheService(this);
    }

    @Override
    protected void doUnExport() {
        //获取注册中心client, 并取消发布服务
        for (RegistryConfig registryConfig : config.getRegistries()) {
            Registry registry = RegistryHelper.getRegistry(registryConfig);

            if (Objects.isNull(registry)) {
                continue;
            }

            registry.unregister(config);
            registry.destroy();
        }

        //service destroy
        rpcService.destroy();
        //shutdown server
        exporter.unExport();
    }
}
