package org.kin.kinrpc.cluster.loadbalance;

/**
 * LoadBalance冲突异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class LoadBalanceConflictException extends RuntimeException {
    public LoadBalanceConflictException(Class<? extends LoadBalance> class1,
                                        Class<? extends LoadBalance> class2) {
        super(String.format("loadBalance name conflict! %s, %s", class1.getName(), class2.getName()));
    }
}
