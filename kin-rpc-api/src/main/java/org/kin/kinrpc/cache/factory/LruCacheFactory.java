package org.kin.kinrpc.cache.factory;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.cache.impl.LruCache;
import org.kin.kinrpc.config.AttachableConfig;
import org.kin.kinrpc.constants.CacheConstants;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
@Extension("lru")
public class LruCacheFactory extends AbstractCacheFactory {
    @Override
    protected Cache doCreateCache(AttachableConfig attachments) {
        int maxSize = attachments.intAttachment(CacheConstants.LRU_CACHE_MAX_SIZE, CacheConstants.DEFAULT_LRU_CACHE_MAX_SIZE);
        return new LruCache(maxSize);
    }
}
