package org.kin.kinrpc.cluster.loadbalance;

import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.rpc.AsyncInvoker;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/29
 * <p>
 * 最不经常使用
 */
public class LFULoadBalance implements LoadBalance {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Integer> lfuMap = new HashMap<>();
    private int monitorTime;

    @Override
    public AsyncInvoker loadBalance(List<AsyncInvoker> invokers) {
        synchronized (lfuMap) {
            int now = TimeUtils.timestamp();
            if (now >= monitorTime + EXPIRE_TIME) {
                monitorTime = now;
                lfuMap.clear();
            }

            //put
            Map<String, AsyncInvoker> address2Invoker = new HashMap<>(invokers.size());
            for (AsyncInvoker AsyncInvoker : invokers) {
                String hostAndPortStr = AsyncInvoker.url().getAddress().toString();
                address2Invoker.put(hostAndPortStr, AsyncInvoker);
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
}
