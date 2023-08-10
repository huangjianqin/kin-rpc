package org.kin.kinrpc.cache.factory;

import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.config.AttachableConfig;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.InvocationConstants;

import java.util.HashMap;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
public abstract class AbstractCacheFactory implements CacheFactory {
    /** {@link Cache}缓存 */
    private final CopyOnWriteMap<Integer, Cache> cacheMap = new CopyOnWriteMap<>(() -> new HashMap<>(16));

    @Override
    public final Cache createCache(Invocation invocation) {
        MethodConfig methodConfig = invocation.attachment(InvocationConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            throw new IllegalStateException("can not find method config. invocation=" + invocation);
        }

        return cacheMap.computeIfAbsent(invocation.handlerId(), k -> doCreateCache(methodConfig));
    }

    /**
     * create cache
     *
     * @param attachments extra cache parameter
     * @return cache
     */
    protected abstract Cache doCreateCache(AttachableConfig attachments);
}
