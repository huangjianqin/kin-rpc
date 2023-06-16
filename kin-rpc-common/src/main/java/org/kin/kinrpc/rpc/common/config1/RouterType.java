package org.kin.kinrpc.rpc.common.config1;

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
