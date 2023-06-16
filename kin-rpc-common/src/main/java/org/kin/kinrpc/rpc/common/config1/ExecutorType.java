package org.kin.kinrpc.rpc.config;

import org.kin.kinrpc.rpc.common.Url;

/**
 * 注意{@link #name}要与executor factory extension name一致
 * @author huangjianqin
 * @date 2021/4/20
 */
public enum ExecutorType{
    CACHE("cache"),
    DIRECT("direct"),
    EAGER("eager"),
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
