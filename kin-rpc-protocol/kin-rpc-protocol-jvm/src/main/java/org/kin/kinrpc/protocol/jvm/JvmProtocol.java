package org.kin.kinrpc.protocol.jvm;

import com.google.common.base.Preconditions;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;
import org.kin.kinrpc.protocol.Protocol;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 在同一jvm内部直接调用
 *
 * @author huangjianqin
 * @date 2020/12/13
 */
@Extension("jvm")
public class JvmProtocol implements Protocol {
    /** key -> service id, value -> service provider invoker */
    private final Map<Integer, RpcService<?>> rpcServiceCache = new CopyOnWriteMap<>();

    @Override
    public <T> Exporter<T> export(RpcService<T> rpcService, ServerConfig serverConfig) {
        String service = rpcService.service();
        int serviceId = rpcService.serviceId();
        if (rpcServiceCache.containsKey(serviceId)) {
            throw new RpcException(String.format("service '%s' has been exported", service));
        }

        rpcServiceCache.put(serviceId, rpcService);

        return new Exporter<T>() {
            @Override
            public RpcService<T> service() {
                return rpcService;
            }

            @Override
            public void unExport() {
                RpcService<T> invoker = service();
                rpcServiceCache.remove(invoker.getConfig().getServiceId());
            }
        };
    }

    @Override
    public <T> ReferenceInvoker<T> refer(ServiceInstance instance, SslConfig sslConfig) {
        return new JvmReferenceInvoker<>(instance);
    }

    @Override
    public void destroy() {
        rpcServiceCache.clear();
    }

    //------------------------------------------------------------------------------------------------------------------------------------------
    private class JvmReferenceInvoker<T> implements ReferenceInvoker<T> {
        /** 服务实例信息 */
        private final ServiceInstance instance;

        public JvmReferenceInvoker(ServiceInstance instance) {
            this.instance = instance;
        }

        @Override
        public RpcResult invoke(Invocation invocation) {
            if (invocation.serviceId() != instance.serviceId()) {
                throw new RpcException(String.format("invocation service(%s) is not right, should be %s", invocation.service(), instance.service()));
            }

            RpcService<?> rpcService = rpcServiceCache.get(invocation.serviceId());
            Preconditions.checkNotNull(rpcService, String.format("can not find service '%s'", invocation.service()));
            CompletableFuture<Object> future = new CompletableFuture<>();
            ReferenceContext.SCHEDULER.execute(() -> rpcService.invoke(invocation).onFinish(future));
            return RpcResult.success(invocation, future);
        }

        @Override
        public ServiceInstance serviceInstance() {
            return instance;
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void destroy() {
            //do nothing
        }
    }
}
