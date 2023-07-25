package org.kin.kinrpc.cache.factory;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.cache.Cache;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
@SPI(alias = "cacheFactory")
public interface CacheFactory {
    /**
     * create cache
     *
     * @param invocation rpc call info
     * @return cache
     */
    Cache createCache(Invocation invocation);
}
