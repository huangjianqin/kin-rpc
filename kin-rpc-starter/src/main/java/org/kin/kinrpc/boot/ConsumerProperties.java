package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.ClusterType;
import org.kin.kinrpc.config.LoadBalanceType;
import org.kin.kinrpc.config.RouterType;
import org.kin.kinrpc.config.SerializationType;
import org.kin.kinrpc.constants.ReferenceConstants;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
public class ConsumerProperties {
    private String serialization = SerializationType.JSON.getName();
    /** 集群处理, 默认是failover */
    private String cluster = ClusterType.FAIL_FAST.getName();
    /** 负载均衡类型, 默认round robin */
    private String loadBalance = LoadBalanceType.ROUND_ROBIN.getName();
    /** 路由类型, 默认none */
    private String router = RouterType.NONE.getName();
    /** 是否是泛化接口引用 */
    private boolean generic;
    /**
     * rpc call timeout(ms)
     */
    private int rpcTimeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
}
