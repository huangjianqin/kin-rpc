package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by huangjianqin on 2019/6/25.
 */
public abstract class AbstractRegistry implements Registry {
    protected final Cache<String, Directory> directoryCache = CacheBuilder.newBuilder().build();

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
