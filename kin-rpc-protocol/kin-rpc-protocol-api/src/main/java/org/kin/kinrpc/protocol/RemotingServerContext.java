package org.kin.kinrpc.protocol;

import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.CommonConstants;
import org.kin.kinrpc.service.MetadataService;
import org.kin.kinrpc.service.impl.MetadataServiceImpl;
import org.kin.kinrpc.transport.RemotingServer;

/**
 * remoting server上下文
 *
 * @author huangjianqin
 * @date 2023/6/28
 */
public class RemotingServerContext {
    /** remoting server */
    private final RemotingServer server;
    /** rpc request processor */
    private final DefaultRpcRequestProcessor rpcRequestProcessor;
    /** 内置元数据服务 */
    private final MetadataServiceImpl metadataService = new MetadataServiceImpl();
    /** 元数据服务对应的{@link RpcService}实例 */
    private final RpcService<MetadataService> metadataRpcService;

    public RemotingServerContext(RemotingServer server, DefaultRpcRequestProcessor rpcRequestProcessor) {
        this.server = server;
        this.rpcRequestProcessor = rpcRequestProcessor;

        ServiceConfig<MetadataService> metadataServiceConfig = ServiceConfig.create(MetadataService.class, metadataService)
                .group(CommonConstants.INTERNAL_SERVICE_GROUP)
                .serviceName(CommonConstants.METADATA_SERVICE_NAME)
                .version(CommonConstants.INTERNAL_SERVICE_VERSION);
        metadataRpcService = new RpcService<>(metadataServiceConfig);
        rpcRequestProcessor.register(metadataRpcService);
    }

    /**
     * register rpc service to server
     *
     * @param rpcService {@link RpcService}实例
     */
    public void register(RpcService<?> rpcService) {
        //注册元数据
        metadataService.register(rpcService.getConfig());
        rpcRequestProcessor.register(rpcService);
    }

    /**
     * unregister rpc service from server
     *
     * @param serviceId 服务唯一id
     */
    public void unregister(int serviceId) {
        rpcRequestProcessor.unregister(serviceId);
    }

    /**
     * remoting server shutdown
     */
    public void shutdown() {
        metadataRpcService.destroy();
        server.shutdown();
    }

    //getter
    public RemotingServer getServer() {
        return server;
    }

    public DefaultRpcRequestProcessor getRpcRequestProcessor() {
        return rpcRequestProcessor;
    }
}
