package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public enum ClusterType {
    /** failover */
    FAILOVER("failover"),
    /** failfast */
    FAIL_FAST("failfast"),
    ;
    private final String name;

    ClusterType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
