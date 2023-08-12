package org.kin.kinrpc.registry.etcd;

/**
 * @author huangjianqin
 * @date 2023/8/12
 */
public final class EtcdConstants {
    /** etcd user */
    public static final String USER_KEY = "user";

    /** etcd password */
    public static final String PASSWORD_KEY = "password";

    /** etcd authority */
    public static final String AUTHORITY_KEY = "authority";

    /** etcd namespace */
    public static final String NAMESPACE_KEY = "namespace";

    /** etcd keepaliveTime */
    public static final String KEEP_ALIVE_TIME_KEY = "keepaliveTime";

    /** etcd keepaliveTimeout */
    public static final String KEEP_ALIVE_TIMEOUT_KEY = "keepaliveTimeout";

    /** etcd keepaliveWithoutCalls */
    public static final String KEEP_ALIVE_WITHOUT_CALLS_KEY = "keepaliveWithoutCalls";

    /** etcd connectTimeout */
    public static final String CONNECT_TIMEOUT_KEY = "connectTimeout";

    /** etcd retryMaxAttempts */
    public static final String RETRY_MAX_ATTEMPTS_KEY = "retryMaxAttempts";

    //-----------------------------------------------------------------------------------------------default

    private EtcdConstants() {
    }
}
