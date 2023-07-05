package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/7/30
 */
public enum RegistryType {
    /** url直连 */
    DIRECT("direct"),
    /** zookeeper */
    ZOOKEEPER("zk"),
    /** nacos */
    NACOS("nacos"),
    /** etcd */
    ETCD("etcd"),
    /** consul */
    CONSUL("consul"),
    /** kubernetes */
    K8S("k8s"),
    ;
    private final String name;

    RegistryType(String name) {
        this.name = name;
    }

    //getter
    public String getName() {
        return name;
    }
}
