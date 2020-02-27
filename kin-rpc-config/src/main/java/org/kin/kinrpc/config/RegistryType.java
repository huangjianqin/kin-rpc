package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RegistryType {
    /**
     * 自带api的zookeeper注册中心
     */
    ZOOKEEPER,
    /**
     * 使用curator框架的zookeeper注册中心
     */
    ZOOKEEPER2,
    /**
     * hession序列化
     */
    DIRECTURLS,
    ;

    public String getType() {
        return name().toLowerCase();
    }
}
