package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2021/4/20
 */
public enum ExecutorType{
    /** cache executor */
    CACHE("cache"),
    /** eager executor */
    EAGER("eager"),
    /** fix executor */
    FIX("fix"),
    ;
    private final String name;

    ExecutorType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
