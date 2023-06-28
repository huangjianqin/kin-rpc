package org.kin.kinrpc.protocol.jvm;

import com.google.common.base.Preconditions;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.ServiceConfig;
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
    private final Map<Integer, ServiceInvoker<?>> serviceInvokerMap = new CopyOnWriteMap<>();

    @Override
    public <T> Exporter<T> export(ServiceConfig<T> serviceConfig) {
        ServiceInvoker<T> invoker = new ServiceInvoker<>(serviceConfig);
        serviceInvokerMap.put(serviceConfig.serviceId(), invoker);

        return new Exporter<T>() {
            @Override
            public ServiceInvoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void unexport() {
                ServiceInvoker<T> invoker = getInvoker();
                serviceInvokerMap.remove(invoker.getConfig().serviceId());
            }
        };
    }

    @Override
    public <T> ReferenceInvoker<T> refer(ServiceInstance instance) {
        return new JvmReferenceInvoker<>(instance);
    }

    @Override
    public void destroy() {
        serviceInvokerMap.clear();
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

            ServiceInvoker<?> serviceInvoker = serviceInvokerMap.get(invocation.serviceId());
            Preconditions.checkNotNull(serviceInvoker, "can not find service invoker for service " + invocation.service());
            CompletableFuture<Object> future = new CompletableFuture<>();
            ReferenceContext.EXECUTOR.execute(() -> serviceInvoker.invoke(invocation).onFinish(future));
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
