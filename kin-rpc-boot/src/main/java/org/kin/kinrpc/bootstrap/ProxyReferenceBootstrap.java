package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.KinRpcAppContext;
import org.kin.kinrpc.cluster.ReferenceProxy;
import org.kin.kinrpc.cluster.utils.ByteBuddyUtils;
import org.kin.kinrpc.config.ReferenceConfig;

import java.lang.reflect.Proxy;

/**
 * reference bootstrap for create service reference base on proxy
 *
 * @author huangjianqin
 * @date 2023/7/2
 */
public abstract class ProxyReferenceBootstrap<T> extends ReferenceBootstrap<T> {
    protected ProxyReferenceBootstrap(ReferenceConfig<T> config) {
        super(config);
    }

    /**
     * 创建reference代理
     *
     * @param invoker invoker
     * @return reference代理实例
     */
    @SuppressWarnings("unchecked")
    protected T createProxy(Invoker<T> invoker) {
        ReferenceProxy referenceProxy = new ReferenceProxy(config, invoker);

        Class<T> interfaceClass = config.getInterfaceClass();
        if (KinRpcAppContext.ENHANCE) {
            reference = ByteBuddyUtils.build(interfaceClass, referenceProxy);
        } else {
            reference = (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(),
                    new Class[]{interfaceClass},
                    referenceProxy);
        }

        return reference;
    }
}
