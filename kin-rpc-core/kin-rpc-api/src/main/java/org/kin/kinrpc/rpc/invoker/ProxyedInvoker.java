package org.kin.kinrpc.rpc.invoker;

import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.CompletableFuture;

/**
 * 代理invoker的invoker
 *
 * @author huangjianqin
 * @date 2020/11/4
 */
public class ProxyedInvoker<T> implements AsyncInvoker<T> {
    /** 包装的invoker */
    protected final Invoker<T> proxy;
    /** 额外的destroy逻辑 */
    private Runnable destroyRunner;

    public ProxyedInvoker(Invoker<T> proxy) {
        this(proxy, null);
    }

    public ProxyedInvoker(Invoker<T> proxy, Runnable destroyRunner) {
        this.proxy = proxy;
        this.destroyRunner = destroyRunner;
    }

    /**
     * 创建代理{@link ProviderInvoker}的invoker
     */
    public static <T> ProxyedInvoker<T> proxyedProviderInvoker(Url url, T instance, Class<T> interfaceClass, boolean byteCodeInvoke) {
        return proxyedProviderInvoker(url, instance, interfaceClass, byteCodeInvoke, null);
    }

    /**
     * 创建代理{@link ProviderInvoker}的invoker
     *
     * @param url            service 描述
     * @param instance       service 实例
     * @param interfaceClass service接口
     * @param byteCodeInvoke 是否使用代码增强
     * @param destroyRunner  额外的destroy逻辑
     */
    public static <T> ProxyedInvoker<T> proxyedProviderInvoker(Url url, T instance, Class<T> interfaceClass, boolean byteCodeInvoke, Runnable destroyRunner) {
        if (byteCodeInvoke) {
            return new ProxyedInvoker<>(new JavassistProviderInvoker<>(url, instance, interfaceClass), destroyRunner);
        } else {
            return new ProxyedInvoker<>(new JdkProxyProviderInvoker<>(url, instance, interfaceClass), destroyRunner);
        }
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
    }
}
