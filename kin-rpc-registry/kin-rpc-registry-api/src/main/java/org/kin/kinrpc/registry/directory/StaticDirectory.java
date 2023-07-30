package org.kin.kinrpc.registry.directory;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.ReferenceInvoker;

import java.util.Collections;
import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/30
 */
public class StaticDirectory extends AbstractDirectory {
    /** reference/cluster invoker */
    private final List<ReferenceInvoker<?>> invokers;

    public StaticDirectory(List<ReferenceInvoker<?>> invokers) {
        if (CollectionUtils.isEmpty(invokers)) {
            throw new IllegalArgumentException("invoker list must be not empty");
        }
        this.invokers = Collections.unmodifiableList(invokers);
    }

    @Override
    public List<ReferenceInvoker<?>> list() {
        return invokers;
    }

    @Override
    public boolean isAvailable() {
        for (ReferenceInvoker<?> invoker : invokers) {
            if (invoker.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        for (ReferenceInvoker<?> invoker : invokers) {
            invoker.destroy();
        }
    }

    @Override
    public String service() {
        return invokers.get(0).serviceInstance().service();
    }
}
