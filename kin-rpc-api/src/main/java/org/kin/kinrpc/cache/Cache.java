package org.kin.kinrpc.cache;

/**
 * rpc result cache
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public interface Cache {
    /**
     * cache rpc result
     *
     * @param key   key(rpc handler + param)
     * @param value rpc result
     */
    void put(String key, Object value);

    /**
     * get cached rpc result
     *
     * @param key key(rpc handler + param)
     * @return cached rpc result
     */
    <R> R get(String key);
}
