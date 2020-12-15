package org.kin.kinrpc.rpc.invoker;

import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Url;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * 代理{@link ProviderInvoker}的{@link AsyncInvoker}
 *
 * @author huangjianqin
 * @date 2020/12/15
 */
public class ProxyedProviderInvoker<T> implements AsyncInvoker<T> {
    /** 代理类 */
    private ProviderInvoker<T> proxy;
    /** 额外的destroy逻辑 */
    private Runnable destroyRunner;

    public ProxyedProviderInvoker(Url url, T instance, Class<T> interfaceClass, boolean byteCodeInvoke) {
        this(url, instance, interfaceClass, byteCodeInvoke, null);
    }

    public ProxyedProviderInvoker(Url url, T instance, Class<T> interfaceClass, boolean byteCodeInvoke, Runnable destroyRunner) {
        if (byteCodeInvoke) {
            proxy = new JavassistProviderInvoker<>(url, instance, interfaceClass);
        } else {
            proxy = new ReflectProviderInvoker<>(url, instance, interfaceClass);
        }
        this.destroyRunner = destroyRunner;
    }

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
        return proxy.invoke(methodName, params);
    }

    @Override
    public Class<T> getInterface() {
        return proxy.getInterface();
    }

    @Override
    public Url url() {
        return proxy.url();
    }

    @Override
    public boolean isAvailable() {
        return proxy.isAvailable();
    }

    @Override
    public void destroy() {
        proxy.destroy();

        if (Objects.nonNull(destroyRunner)) {
            destroyRunner.run();
        }
    }
}
