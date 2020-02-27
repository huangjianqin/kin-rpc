package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RouterType {
    /**
     * 不router
     */
    NONE,
    ;

    public String getType() {
        return name().toLowerCase();
    }
}
