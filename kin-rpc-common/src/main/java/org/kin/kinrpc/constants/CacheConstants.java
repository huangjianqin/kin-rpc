package org.kin.kinrpc.constants;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
public final class CacheConstants {
    /** lru缓存大小 */
    public static final String LRU_CACHE_MAX_SIZE = "cache.lru.size";
    /** 缓存存活时间(秒) */
    public static final String EXPIRING_CACHE_TTL = "cache.expiring.ttl";
    /** 支持过期缓存清理间隔(毫秒) */
    public static final String EXPIRING_CACHE_EXPIRATION_INTERVAL = "cache.expiring.expiration.interval";


    //--------------------------------------------------------------------default
    /** lru缓存默认最大大小 */
    public static final int DEFAULT_LRU_CACHE_MAX_SIZE = 1000;
    /** 默认缓存存活时间(毫秒) */
    public static final int DEFAULT_EXPIRING_CACHE_TTL = (int) TimeUnit.SECONDS.toMillis(180);
    /** 支持过期缓存默认缓存清理间隔(毫秒) */
    public static final int DEFAULT_EXPIRING_CACHE_EXPIRATION_INTERVAL = 1000;

    private CacheConstants() {
    }
}
