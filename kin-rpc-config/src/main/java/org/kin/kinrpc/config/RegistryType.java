package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RegistryType {
    /**
     * zookeeper注册中心
     */
    ZOOKEEPER,
    /**
     * url直连
     */
    DIRECTURLS,

    /**
     * redis注册中心
     */
    REDIS,
    ;

    public String getType() {
        return name().toLowerCase();
    }
}
