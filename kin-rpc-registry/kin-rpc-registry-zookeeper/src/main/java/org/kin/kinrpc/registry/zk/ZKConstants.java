package org.kin.kinrpc.registry.zk;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public final class ZKConstants {
    /** zookeeper会话超时 */
    public static final String SESSION_TIMEOUT_KEY = "sessionTimeout";
    /** zookeeper auth schema */
    public static final String AUTH_SCHEMA_KEY = "authSchema";
    /** zookeeper auth */
    public static final String AUTH_KEY = "auth";

    //-----------------------------------------------------------------------------------------------default

    /** zookeeper会话超时 */
    public static final int DEFAULT_SESSION_TIMEOUT = 30000;

    private ZKConstants() {
    }
}
