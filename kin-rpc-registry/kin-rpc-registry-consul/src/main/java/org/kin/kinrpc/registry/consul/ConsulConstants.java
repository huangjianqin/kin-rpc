package org.kin.kinrpc.registry.consul;

/**
 * @author huangjianqin
 * @date 2023/8/12
 */
public final class ConsulConstants {
    /** 发送consul心跳间隔 */
    public static final String CHECK_PASS_INTERVAL_KEY = "checkPassInterval";
    /** 服务ttl多久后直接取消注册 */
    public static final String DEREGISTER_CRITICAL_SERVICE_AFTER_KEY = "deregisterCriticalServiceAfter";
    /** acl token */
    public static final String ACL_TOKEN_KEY = "aclToken";
    /** common request query tag */
    public static final String QUERY_TAG_KEY = "queryTag";

    //-----------------------------------------------------------------------------------------------default

    /** 默认发送consul心跳间隔 */
    public static final int DEFAULT_CHECK_PASS_INTERVAL = 40_000;
    /** 默认服务ttl 20s后直接取消注册 */
    public static final String DEFAULT_DEREGISTER_CRITICAL_SERVICE_AFTER = "20s";

    private ConsulConstants() {
    }
}
