package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RouterType {
    /**
     * 不router
     */
    NONE("none"),
    ;

    RouterType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }
}
