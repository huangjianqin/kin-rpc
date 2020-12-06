package org.kin.kinrpc.cluster;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.rpc.common.RpcServiceLoader;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class LoadBalances {
    /** key-> class name, value -> LoadBalance instance */
    private static volatile Map<String, LoadBalance> LOADBALANCE_CACHE = Collections.emptyMap();

    static {
        initLoad();
    }

    private LoadBalances() {
    }

    /**
     * 缓存通过SPI加载的实例
     */
    private static void initLoad() {
        Map<String, LoadBalance> loadBalanceCache = new HashMap<>();

        //通过spi机制加载自定义的LoadBalance
        List<LoadBalance> loadBalances = RpcServiceLoader.LOADER.getExtensions(LoadBalance.class);
        for (LoadBalance loadBalance : loadBalances) {
            Class<? extends LoadBalance> claxx = loadBalance.getClass();
            String prefixName = ClassUtils.getPrefixName(claxx, LoadBalance.class);
            if (StringUtils.isNotBlank(prefixName)) {
                if (loadBalanceCache.containsKey(prefixName)) {
                    throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(prefixName).getClass());
                }
                loadBalanceCache.put(prefixName, loadBalance);
            }

            String className = claxx.getName();

            if (loadBalanceCache.containsKey(className)) {
                throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(className).getClass());
            }

            loadBalanceCache.put(className, loadBalance);
        }

        LOADBALANCE_CACHE = Collections.unmodifiableMap(loadBalanceCache);

    }

    /**
     * 尝试加载指定类型的LoadBalance
     */
    private static synchronized LoadBalance loadNew(String type) {
        if (LOADBALANCE_CACHE.containsKey(type)) {
            return LOADBALANCE_CACHE.get(type);
        }

        LoadBalance loadBalance = null;
        Map<String, LoadBalance> loadBalanceCache = new HashMap<>(LOADBALANCE_CACHE);
        //加载内部提供的LoadBalance
        Set<Class<? extends LoadBalance>> classes = ClassUtils.getSubClass(LoadBalance.class.getPackage().getName(), LoadBalance.class, true);
        if (classes.size() > 0) {
            for (Class<? extends LoadBalance> claxx : classes) {
                String prefixName = ClassUtils.getPrefixName(claxx, LoadBalance.class);
                String className = claxx.getName();

                if (!type.equals(prefixName) && !type.equals(className)) {
                    continue;
                }

                loadBalance = ClassUtils.instance(claxx);
                if (StringUtils.isNotBlank(prefixName)) {
                    if (loadBalanceCache.containsKey(prefixName)) {
                        throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(prefixName).getClass());
                    }
                    loadBalanceCache.put(prefixName, loadBalance);
                }
                if (loadBalanceCache.containsKey(className)) {
                    throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(className).getClass());
                }

                loadBalanceCache.put(className, loadBalance);
            }
        }
        LOADBALANCE_CACHE = Collections.unmodifiableMap(loadBalanceCache);

        return loadBalance;
    }

    /**
     * 根据LoadBalance name 获取LoadBalance instance
     */
    public static LoadBalance getLoadBalance(String type) {
        if (StringUtils.isBlank(type)) {
            throw new IllegalArgumentException("LoadBalance type is blank");
        }

        if (LOADBALANCE_CACHE.containsKey(type)) {
            return LOADBALANCE_CACHE.get(type);
        } else {
            LoadBalance loadBalance = loadNew(type);
            if (Objects.nonNull(loadBalance)) {
                return loadBalance;
            }
        }

        throw new IllegalArgumentException(String.format("unknown LoadBalance or unable to load LoadBalance '%s'", type));
    }
}
