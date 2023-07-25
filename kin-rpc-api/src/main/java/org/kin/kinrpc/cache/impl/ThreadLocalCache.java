package org.kin.kinrpc.cache.impl;

import org.kin.kinrpc.cache.Cache;

import java.util.HashMap;
import java.util.Map;

/**
 * thread local cache
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public class ThreadLocalCache implements Cache {
    /** thread local thread */
    private static final ThreadLocal<Map<String, Object>> THREAD_LOCAL_CACHE = ThreadLocal.withInitial(HashMap::new);

    @Override
    public void put(String key, Object value) {
        THREAD_LOCAL_CACHE.get().put(key, value);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> R get(String key) {
        return (R) THREAD_LOCAL_CACHE.get().get(key);
    }
}
