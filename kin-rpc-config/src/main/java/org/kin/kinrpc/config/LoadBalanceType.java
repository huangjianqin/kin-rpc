package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum LoadBalanceType {
    /**
     * hash
     */
    HASH("hash"),
    /**
     * lfu, 最不经常使用
     */
    LFU("lfu"),
    /**
     * lru, 最近最少使用
     */
    LRU("lru"),
    /**
     * random
     */
    RANDOM("random"),
    /**
     * round robin
     */
    ROUND_ROBIN("roundrobin"),
    ;

    LoadBalanceType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }
}
