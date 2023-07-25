package org.kin.kinrpc.cache.factory;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.cache.impl.ThreadLocalCache;
import org.kin.kinrpc.config.AttachableConfig;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
@Extension("threadLocal")
public class ThreadLocalCacheFactory extends AbstractCacheFactory {
    @Override
    protected Cache doCreateCache(AttachableConfig attachments) {
        return new ThreadLocalCache();
    }
}
