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

    private static String getKey(String className) {
        int index = className.indexOf(LoadBalance.class.getSimpleName());
        if (index > 0) {
            className = className.substring(0, index);
        }

        return className.toLowerCase();
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
                String key = getKey(claxx.getSimpleName());
                if (loadBalanceCache.containsKey(key)) {
                    throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(key).getClass());
                }
                try {
                    LoadBalance loadBalance = claxx.newInstance();
                    loadBalanceCache.put(key, loadBalance);
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
            String key = getKey(claxx.getSimpleName());
            if (loadBalanceCache.containsKey(key)) {
                throw new LoadBalanceConflictException(claxx, loadBalanceCache.get(key).getClass());
            }

            loadBalanceCache.put(key, loadBalance);
        }

        LOADBALANCE_CACHE = Collections.unmodifiableMap(loadBalanceCache);
    }

    /**
     * 根据LoadBalance name 获取LoadBalance instance
     */
    public static LoadBalance getLoadBalance(String type) {
        return LOADBALANCE_CACHE.get(type);
    }

    /**
     * 根据LoadBalance class 返回LoadBalance name, 如果没有加载到, 则主动重新加载
     */
    public static synchronized String getOrLoadLoadBalance(Class<? extends LoadBalance> loadBalanceClass) {
        String key = getKey(loadBalanceClass.getSimpleName());
        if (LOADBALANCE_CACHE.containsKey(key)) {
            //已加载
            return key;
        }

        Map<String, LoadBalance> loadBalanceCache = new HashMap<>(LOADBALANCE_CACHE);
        //未加载
        try {
            LoadBalance loadBalance = loadBalanceClass.newInstance();
            loadBalanceCache.put(key, loadBalance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        LOADBALANCE_CACHE = Collections.unmodifiableMap(loadBalanceCache);

        return key;
    }
}
