package org.kin.kinrpc;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.Executor;

/**
 * 服务元数据
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class ServiceMetadata {
    private static final Logger log = LoggerFactory.getLogger(ServiceMetadata.class);

    /** 服务唯一id */
    private final int serviceId;
    /** 服务唯一标识 */
    private final String service;
    /** 服务方法元数据 */
    private final IntObjectMap<MethodMetadata> methodMetadataMap;
    /** 服务invoker */
    private final Invoker<?> invoker;
    /** 服务调用线程池 */
    private final Executor executor;

    public ServiceMetadata(ServiceConfig<?> config,
                           Invoker<?> invoker,
                           Executor executor) {
        this.service = GsvUtils.service(config.getGroup(), config.getServiceName(), config.getVersion());
        this.serviceId = GsvUtils.serviceId(this.service);
        this.methodMetadataMap = RpcUtils.getMethodMetadataMap(this.service, config.getInterfaceClass());
        this.invoker = invoker;
        this.executor = executor;

        for (MethodMetadata methodMetadata : methodMetadataMap) {
            log.info("export service handler, service={}, serviceId={}, handler={}, handlerId={}, method={}",
                    service,
                    serviceId,
                    methodMetadata.handler(),
                    methodMetadata.handlerId(),
                    methodMetadata.method());
        }
    }

    /**
     * 返回服务方法元数据
     *
     * @param handlerId 服务方法唯一id
     * @return 服务方法元数据
     */
    @Nullable
    public MethodMetadata getMethodMetadata(int handlerId) {
        return methodMetadataMap.get(handlerId);
    }

    public int serviceId() {
        return serviceId;
    }

    public String service() {
        return service;
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public Executor getExecutor() {
        return executor;
    }
}
