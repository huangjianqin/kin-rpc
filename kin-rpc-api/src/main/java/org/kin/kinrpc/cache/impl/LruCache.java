package org.kin.kinrpc.cache.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.constants.CacheConstants;

/**
 * least recently used cache
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public class LruCache implements Cache {
    private final LRUMap<String, Object> lruMap;

    public LruCache() {
        this(CacheConstants.DEFAULT_LRU_CACHE_MAX_SIZE);
    }

    public LruCache(int maxItemNum) {
        this.lruMap = new LRUMap<>(maxItemNum);
    }

    @Override
    public void put(String key, Object value) {
        lruMap.put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R get(String key) {
        return (R) lruMap.get(key);
    }
}
