package org.kin.kinrpc.cluster.loadbalance.impl;

import org.kin.framework.collection.LRUMap;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019/7/29
 * <p>
 * 最近最少使用
 */
public class LRULoadBalance implements LoadBalance {
    private static final int EXPIRE_TIME = (int) TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES);

    private Map<String, Boolean> lruMap = new LRUMap<>(19);
    private int monitorTime;

    @Override
    public ReferenceInvoker loadBalance(List<ReferenceInvoker> invokers) {
        synchronized (lruMap) {
            int now = TimeUtils.timestamp();
            if (now >= monitorTime + EXPIRE_TIME) {
                monitorTime = now;
                lruMap.clear();
            }

            //put
            Map<String, ReferenceInvoker> address2Invoker = new HashMap<>(invokers.size());
            for (ReferenceInvoker invoker : invokers) {
                address2Invoker.put(invoker.getAddress().toString(), invoker);
                lruMap.put(invoker.getAddress().toString(), true);
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
}
