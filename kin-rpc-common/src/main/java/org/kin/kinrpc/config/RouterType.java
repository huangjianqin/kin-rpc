package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RouterType {
    /** 不进行router */
    NONE("none"),
    ;
    private final String name;

    RouterType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
