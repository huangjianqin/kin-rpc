package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RegistryType {
    /**
     * 自带api的zookeeper注册中心
     */
    ZOOKEEPER("zookeeper"),
    /**
     * 使用curator框架的zookeeper注册中心
     */
    ZOOKEEPER2("zookeeper2"),
    /**
     * hession序列化
     */
    DIRECTURLS("directurls"),
    ;

    RegistryType(String type) {
        this.type = type;
    }

    private String type;

    public String getType() {
        return type;
    }
}
