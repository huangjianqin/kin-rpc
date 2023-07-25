package org.kin.kinrpc.cache.factory;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.cache.Cache;
import org.kin.kinrpc.cache.impl.ExpiringCache;
import org.kin.kinrpc.config.AttachableConfig;
import org.kin.kinrpc.constants.CacheConstants;

/**
 * @author huangjianqin
 * @date 2023/7/24
 */
@Extension("expiring")
public class ExpiringCacheFactory extends AbstractCacheFactory {
    @Override
    protected Cache doCreateCache(AttachableConfig attachments) {
        int ttl = attachments.intAttachment(CacheConstants.EXPIRING_CACHE_TTL,
                CacheConstants.DEFAULT_EXPIRING_CACHE_TTL);
        int expirationInterval = attachments.intAttachment(CacheConstants.EXPIRING_CACHE_EXPIRATION_INTERVAL,
                CacheConstants.DEFAULT_EXPIRING_CACHE_EXPIRATION_INTERVAL);
        return new ExpiringCache(ttl, expirationInterval);
    }
}
