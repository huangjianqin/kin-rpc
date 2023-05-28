package org.kin.kinrpc.transport.jvm;

import com.google.common.base.Preconditions;
import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.Extension;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;
import org.kin.kinrpc.rpc.Protocol;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在同一jvm内部直接调用
 *
 * @author huangjianqin
 * @date 2020/12/13
 */
@Extension("jvm")
public class JvmProtocol implements Protocol, LoggerOprs {
    /** key -> service id, value -> service provider invoker */
    private final Map<Integer, ProviderInvoker<?>> providers = new ConcurrentHashMap<>();

    @Override
    public <T> Exporter<T> export(ProviderInvoker<T> invoker) {
        Url url = invoker.url();
        providers.put(url.getServiceId(), invoker);

        info("jvm service '{}' export address '{}'", url.getServiceKey(), url.getAddress());

        return new Exporter<T>() {
            @Override
            public Invoker<T> getInvoker() {
                return invoker;
            }

            @Override
            public void unexport() {
                providers.remove(url.getServiceId());
                invoker.destroy();
            }
        };
    }

    @Override
    public <T> AsyncInvoker<T> refer(Url url) {
        info("jvm reference '{}' refer address '{}'", url.getService(), url.getAddress());

        return new AsyncInvoker<T>() {
            @Override
            public CompletableFuture<Object> invokeAsync(String methodName, Object... params) {
                return CompletableFuture.supplyAsync(() -> {
                    try {
                        return invoke(methodName, params);
                    } catch (Throwable throwable) {
                        throw new IllegalStateException(throwable);
                    }
                }, RpcThreadPool.executors());
            }

            @Override
            public Object invoke(String methodName, Object[] params) throws Throwable {
                ProviderInvoker<?> providerInvoker = providers.get(url.getServiceId());
                Preconditions.checkNotNull(providerInvoker, "can not find valid invoker");
                try {
                    return providerInvoker.invoke(methodName, params);
                } catch (Throwable throwable) {
                    ExceptionUtils.throwExt(throwable);
                }

                throw new IllegalStateException("encounter unknown error");
            }

            @SuppressWarnings("unchecked")
            @Override
            public Class<T> getInterface() {
                try {
                    return (Class<T>) Class.forName(url.getInterfaceName());
                } catch (ClassNotFoundException e) {
                    ExceptionUtils.throwExt(e);
                }
                throw new IllegalStateException("encounter unknown error");
            }

            @Override
            public Url url() {
                return url;
            }

            @Override
            public boolean isAvailable() {
                return providers.containsKey(url.getServiceId());
            }

            @Override
            public void destroy() {
                //do nothing
            }
        };
    }

    @Override
    public void destroy() {
        providers.clear();
    }
}
