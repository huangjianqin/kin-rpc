package org.kin.kinrpc.cache.impl;

import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.constants.CacheConstants;

/**
 * expire support cache
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public class ExpiringCache implements Cache {
    /** expire support cache */
    private final ExpiringMap<String, Object> cache;

    public ExpiringCache(int ttl) {
        this(ttl, CacheConstants.DEFAULT_EXPIRING_CACHE_TTL);
    }

    public ExpiringCache(int ttl, int expirationInterval) {
        cache = new ExpiringMap<>(ttl, expirationInterval);
    }

    @Override
    public void put(String key, Object value) {
        cache.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R get(String key) {
        return (R) cache.get(key);
    }
}
