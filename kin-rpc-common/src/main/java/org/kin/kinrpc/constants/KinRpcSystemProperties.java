package org.kin.kinrpc.constants;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public final class KinRpcSystemProperties {
    //------------------------------------------------------------------------------------registry metadata
    /** 应用实例服务元数据刷新间隔 */
    public static final String METADATA_REFRESH_DELAY = "kinrpc.metadata.refresh.delay";

    //------------------------------------------------------------------------------------cluster
    /** failback cluster重试间隔 */
    public static final String FAILBACK_RETRY_PERIOD = "kinrpc.cluster.failback.period";

    private KinRpcSystemProperties() {
    }
}
