package org.kin.kinrpc.cluster.loadbalance;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class LoadBalances {
    private static final Logger log = LoggerFactory.getLogger(LoadBalances.class);
    private static final Cache<String, LoadBalance> LOADBALANCE_CACHE = CacheBuilder.newBuilder().build();

    private LoadBalances(){
    }

    public static LoadBalance getLoadBalance(String type){
        //从整个classpath寻找LoadBalance子类
        type = type.toLowerCase();
        try {
            String loadBalanceName = (type + LoadBalance.class.getSimpleName()).toLowerCase();
            LoadBalance loadBalance = LOADBALANCE_CACHE.get(type, () -> {
                Set<Class<LoadBalance>> classes = ClassUtils.getSubClass("", LoadBalance.class, true);
                if (classes.size() > 0) {
                    for (Class<LoadBalance> claxx : classes) {
                        String className = claxx.getSimpleName().toLowerCase();
                        if (className.equals(loadBalanceName)) {
                            return claxx.newInstance();
                        }
                    }
                }

                return null;
            });

            return loadBalance;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        throw new IllegalStateException("init loadbalance error >>>" + type);
    }
}
