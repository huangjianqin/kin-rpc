package org.kin.kinrpc.cache.impl;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.constants.CacheConstants;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * concurrent hash map with expire support
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public class ExpiringMap<K, V> implements Map<K, V> {
    /** expire cache cleaner */
    private static final ScheduledExecutorService SCHEDULER = ThreadPoolUtils.newScheduledThreadPool("kinrpc-cache-scheduler", true,
            SysUtils.CPU_NUM / 2 + 1, new SimpleThreadFactory("kinrpc-cache-scheduler", true), new ThreadPoolExecutor.CallerRunsPolicy());

    /** entry cache */
    private final ConcurrentMap<K, ExpiryEntry> delegateMap = new ConcurrentHashMap<>(16);
    /** cache timeToLive(ms) */
    private final int ttl;
    /** expiration check interval(ms) */
    private final int expirationInterval;
    /** process expires future */
    private final ScheduledFuture<?> expirationFuture;

    public ExpiringMap(int ttl) {
        this(ttl, CacheConstants.DEFAULT_EXPIRING_CACHE_TTL);
    }

    public ExpiringMap(int ttl, int expirationInterval) {
        this.ttl = ttl;
        this.expirationInterval = expirationInterval;

        this.expirationFuture = SCHEDULER.scheduleWithFixedDelay(this::processExpires, expirationInterval, expirationInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 处理缓存过期
     */
    private void processExpires() {
        long now = System.currentTimeMillis();
        if (ttl <= 0) {
            return;
        }

        for (ExpiryEntry entry : delegateMap.values()) {
            //空闲时间
            long idle = now - entry.getLastAccessTime();
            if (idle >= ttl) {
                delegateMap.remove(entry.key);
            }
        }
    }

    @Override
    public int size() {
        return delegateMap.size();
    }

    @Override
    public boolean isEmpty() {
        return delegateMap.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return delegateMap.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return delegateMap.containsValue(value);
    }

    @Override
    public V get(Object key) {
        ExpiryEntry entry = delegateMap.get(key);
        if (Objects.nonNull(entry)) {
            long now = System.currentTimeMillis();
            long idle = now - entry.getLastAccessTime();
            if (ttl > 0 && idle >= ttl) {
                delegateMap.remove(entry.getKey());
                return null;
            }
            entry.updateLastAccessTime(now);
            return entry.getValue();
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        ExpiryEntry oldEntry = delegateMap.put(key, new ExpiryEntry(key, value));
        if (Objects.isNull(oldEntry)) {
            return null;
        }
        return oldEntry.value;
    }

    @Override
    public V remove(Object key) {
        ExpiryEntry entry = delegateMap.remove(key);
        if (Objects.isNull(entry)) {
            return null;
        }
        return entry.getValue();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        for (Entry<? extends K, ? extends V> entry : m.entrySet()) {
            put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void clear() {
        delegateMap.clear();
        destroy();
    }

    @Override
    public Set<K> keySet() {
        return delegateMap.keySet();
    }

    @Override
    public Collection<V> values() {
        return delegateMap.values()
                .stream()
                .map(ExpiryEntry::getValue)
                .collect(Collectors.toList());
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * 销毁及释放底层占用资源
     */
    public void destroy() {
        if (expirationFuture.isCancelled()) {
            return;
        }

        expirationFuture.cancel(true);
    }

    //--------------------------------------------------------------------------------
    private class ExpiryEntry {
        /** cache key */
        private K key;
        /** cache value */
        private V value;
        /** last access cache time */
        private AtomicLong lastAccessTime;

        public ExpiryEntry(K key, V value) {
            this.key = key;
            this.value = value;
            this.lastAccessTime = new AtomicLong(System.currentTimeMillis());
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }

        public long getLastAccessTime() {
            return lastAccessTime.get();
        }

        public void updateLastAccessTime(long lastAccessTime) {
            this.lastAccessTime.set(lastAccessTime);
        }

        @Override
        public String toString() {
            return "ExpiryEntry{" +
                    "key=" + key +
                    ", value=" + value +
                    ", lastAccessTime=" + lastAccessTime +
                    '}';
        }
    }
}
