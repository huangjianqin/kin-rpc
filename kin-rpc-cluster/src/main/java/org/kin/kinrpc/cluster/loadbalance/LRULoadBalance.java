package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 基于最近最少使用的负载均衡实现
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
public class LRULoadBalance extends AbstractLoadBalance {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private final ConcurrentHashMap<Integer, LRUMap> lruMaps = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        int key = key(serviceKey, method);
        LRUMap lruMap = lruMaps.computeIfAbsent(key, k -> new LRUMap());
        synchronized (lruMap) {
            int now = TimeUtils.timestamp();
            if (now >= lruMap.monitorTime + EXPIRE_TIME) {
                lruMap.monitorTime = now;
                lruMap.clear();
            }

            //put
            Map<String, AsyncInvoker> address2Invoker = new HashMap<>(invokers.size());
            for (AsyncInvoker AsyncInvoker : invokers) {
                String address = AsyncInvoker.url().getAddress();
                address2Invoker.put(address, AsyncInvoker);
                lruMap.put(address, true);
            }

            //remove invalid
            List<String> invalidAddresses = new ArrayList<>();
            for (String hostAndPortStr : lruMap.keySet()) {
                if (!address2Invoker.containsKey(hostAndPortStr)) {
                    invalidAddresses.add(hostAndPortStr);
                }
            }

            for (String invalidAddress : invalidAddresses) {
                lruMap.remove(invalidAddress);
            }

            String selectedAddress = lruMap.keySet().iterator().next();
            return address2Invoker.get(selectedAddress);
        }
    }

    private static class LRUMap extends org.kin.framework.collection.LRUMap<String, Boolean> {
        private static final long serialVersionUID = 8061853495676788002L;
        private int monitorTime;

        public LRUMap() {
            super(32);
        }
    }
}
