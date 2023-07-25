package org.kin.kinrpc.cache;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Filter;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.Invoker;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.cache.factory.CacheFactory;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.constants.ReferenceConstants;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * process rpc call result cache filter
 *
 * @author huangjianqin
 * @date 2023/7/24
 */
public class CacheFilter implements Filter {
    /** 单例 */
    public static final CacheFilter INSTANCE = new CacheFilter();

    @Override
    public RpcResult invoke(Invoker<?> invoker, Invocation invocation) {
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            //no cache
            return invoker.invoke(invocation);
        }

        String cacheType = methodConfig.getCache();
        if (StringUtils.isBlank(cacheType)) {
            //no cache
            return invoker.invoke(invocation);
        }

        CacheFactory cacheFactory = ExtensionLoader.getExtension(CacheFactory.class, cacheType);
        Cache cache = cacheFactory.createCache(invocation);
        String key = StringUtils.mkString(invocation.params());
        Object value = cache.get(key);
        if (Objects.nonNull(value)) {
            return RpcResult.success(invocation, CompletableFuture.completedFuture(value));
        } else {
            RpcResult rpcResult = invoker.invoke(invocation);
            rpcResult.onFinish((r, t) -> {
                if (Objects.isNull(t)) {
                    //completed
                    cache.put(key, r);
                }
            });
            return rpcResult;
        }
    }
}
