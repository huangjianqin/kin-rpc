package org.kin.kinrpc;

import org.kin.kinrpc.config.AbstractInterfaceConfig;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {
    private final AbstractInterfaceConfig<T, ?> config;

    protected AbstractInvoker(AbstractInterfaceConfig<T, ?> config) {
        this.config = config;
    }

    @Override
    public final AbstractInterfaceConfig<T, ?> config() {
        return config;
    }
}
