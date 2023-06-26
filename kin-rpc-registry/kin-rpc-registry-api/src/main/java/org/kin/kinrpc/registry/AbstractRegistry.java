package org.kin.kinrpc.registry;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by huangjianqin on 2019/6/25.
 */
public abstract class AbstractRegistry implements Registry {
    protected final Cache<String, DefaultDirectory> directoryCache = CacheBuilder.newBuilder().build();

    private AtomicInteger ref = new AtomicInteger(0);
    /** provider or reference的url */
    protected final Url url;

    public AbstractRegistry(Url url) {
        this.url = url;
    }


    @Override
    public void retain() {
        ref.incrementAndGet();
    }

    @Override
    public boolean release() {
        return ref.decrementAndGet() <= 0;
    }
}
