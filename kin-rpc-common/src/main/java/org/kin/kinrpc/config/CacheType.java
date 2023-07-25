package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/7/25
 */
public enum CacheType {
    /** lru cache */
    LRU("lru"),
    /** expiring cache */
    EXPIRING("expiring"),
    /** threadLocal cache */
    THREAD_LOCAL("threadLocal"),
    ;
    private final String name;

    CacheType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
