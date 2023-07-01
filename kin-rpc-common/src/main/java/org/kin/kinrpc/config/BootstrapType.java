package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public enum BootstrapType {
    /** 默认Bootstrap */
    DEFAULT("default"),
    /** jvm Bootstrap */
    JVM("jvm"),
    ;

    private final String name;

    BootstrapType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
