package org.kin.kinrpc.config;

/**
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
    /** Peak EWMA */
    PEAK_EWMA("PeakEWMA"),
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
