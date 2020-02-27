package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum LoadBalanceType {
    /**
     * hash
     */
    HASH,
    /**
     * lfu, 最不经常使用
     */
    LFU,
    /**
     * lru, 最近最少使用
     */
    LRU,
    /**
     * random
     */
    RANDOM,
    /**
     * round robin
     */
    ROUND_ROBIN,
    ;

    public String getType() {
        return name().toLowerCase();
    }
}
