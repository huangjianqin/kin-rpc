package org.kin.kinrpc.registry;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by huangjianqin on 2019/6/25.
 */
public abstract class AbstractRegistry implements Registry {
    private AtomicInteger ref = new AtomicInteger(0);

    @Override
    public void retain() {
        ref.incrementAndGet();
    }

    @Override
    public boolean release() {
        return ref.decrementAndGet() <= 0;
    }
}
