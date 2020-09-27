package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.ClassUtils;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class LoadBalances {
    /** key-> class name, value -> LoadBalance instance */
    private static volatile Map<String, LoadBalance> LOADBALANCE_CACHE = Collections.emptyMap();

    static {
        load();
    }

    private LoadBalances() {
    }

    /**
     * 加载LoadBalance
     */
    private static void load() {
        Map<String, LoadBalance> loadBalanceCache = new HashMap<>();
        //加载内部提供的LoadBalance
        Set<Class<? extends LoadBalance>> classes = ClassUtils.getSubClass(LoadBalance.class.getPackage().getName(), LoadBalance.class, true);
        if (classes.size() > 0) {
            for (Class<? extends LoadBalance> claxx : classes) {
                String className = claxx.getSimpleName().toLowerCase();
                if (loadBalanceCache.containsKey(className)) {
                    throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(className).getClass());
                }
                try {
                    LoadBalance loadBalance = claxx.newInstance();
                    loadBalanceCache.put(className, loadBalance);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //通过spi机制加载自定义的LoadBalance
        ServiceLoader<LoadBalance> serviceLoader = ServiceLoader.load(LoadBalance.class);
        Iterator<LoadBalance> customLoadBalances = serviceLoader.iterator();
        while (customLoadBalances.hasNext()) {
            LoadBalance loadBalance = customLoadBalances.next();
            Class<? extends LoadBalance> claxx = loadBalance.getClass();
            String className = claxx.getSimpleName().toLowerCase();
            if (loadBalanceCache.containsKey(className)) {
                throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(className).getClass());
            }

            loadBalanceCache.put(className, loadBalance);
        }

        LOADBALANCE_CACHE = loadBalanceCache;
    }

    /**
     * 根据LoadBalance name 获取LoadBalance instance
     */
    public static LoadBalance getLoadBalance(String type) {
        return LOADBALANCE_CACHE.get(type);
    }
}
