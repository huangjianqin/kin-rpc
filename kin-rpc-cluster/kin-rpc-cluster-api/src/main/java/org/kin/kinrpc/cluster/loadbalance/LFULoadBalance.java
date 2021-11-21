package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 基于最不经常使用的负载均衡实现
 *
 * @author huangjianqin
 * @date 2019/7/29
 */
public class LFULoadBalance extends AbstractLoadBalance {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private final ConcurrentHashMap<Integer, LFUMap> lfuMaps = new ConcurrentHashMap<>();

    @SuppressWarnings("rawtypes")
    @Override
    public AsyncInvoker loadBalance(String serviceKey, String method, Object[] params, List<AsyncInvoker> invokers) {
        int key = key(serviceKey, method);
        LFUMap lfuMap = lfuMaps.computeIfAbsent(key, k -> new LFUMap());
        synchronized (lfuMap) {
            int now = TimeUtils.timestamp();
            if (now >= lfuMap.monitorTime + EXPIRE_TIME) {
                lfuMap.monitorTime = now;
                lfuMap.clear();
            }

            //put
            Map<String, AsyncInvoker> address2Invoker = new HashMap<>(invokers.size());
            for (AsyncInvoker asyncInvoker : invokers) {
                String hostAndPortStr = asyncInvoker.url().getAddress();
                address2Invoker.put(hostAndPortStr, asyncInvoker);
                if (!lfuMap.containsKey(hostAndPortStr) || lfuMap.get(hostAndPortStr) > 1000000) {
                    //缓解首次的压力
                    lfuMap.put(hostAndPortStr, ThreadLocalRandom.current().nextInt(invokers.size()));
                }
            }

            //remove invalid
            List<String> invalidAddresses = new ArrayList<>();
            for (String hostAndPortStr : lfuMap.keySet()) {
                if (!address2Invoker.containsKey(hostAndPortStr)) {
                    invalidAddresses.add(hostAndPortStr);
                }
            }

            for (String invalidAddress : invalidAddresses) {
                lfuMap.remove(invalidAddress);
            }

            List<Map.Entry<String, Integer>> entries = new ArrayList<>(lfuMap.entrySet());
            entries.sort(Comparator.comparingInt(Map.Entry::getValue));

            String selected = entries.get(0).getKey();
            lfuMap.put(selected, entries.get(0).getValue() + 1);
            return address2Invoker.get(selected);
        }
    }

    private static class LFUMap extends HashMap<String, Integer> {
        private static final long serialVersionUID = 8061853495676788002L;
        private int monitorTime;

        public LFUMap() {
        }
    }
}
