package org.kin.kinrpc.registry.nacos;

/**
 * @author huangjianqin
 * @date 2023/8/12
 */
public final class NacosConstants {
    /** nacos服务实例是否持久化 */
    public static final String EPHEMERAL = "ephemeral";

    //-----------------------------------------------------------------------------------------------default

    /** default nacos cluster name */
    public static final String DEFAULT_CLUSTER_NAME = "kinrpc";

    private NacosConstants() {
    }
}
