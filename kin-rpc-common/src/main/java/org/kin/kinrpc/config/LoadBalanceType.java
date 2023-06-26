package org.kin.kinrpc.config;

/**
 * todo增加基于性能监控动态计算权重的负载均衡算法
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum LoadBalanceType {
    /** hash */
    HASH("hash"),
    /** random */
    RANDOM("random"),
    /** round robin */
    ROUND_ROBIN("roundRobin"),
    ;

    private final String name;

    LoadBalanceType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
