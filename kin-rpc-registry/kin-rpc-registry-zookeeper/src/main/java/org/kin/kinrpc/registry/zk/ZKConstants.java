package org.kin.kinrpc.registry.zk;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public final class ZKConstants {

    /** zookeeper会话超时 */
    public static final String SESSION_TIMEOUT_KEY = "zk.sessionTimeout";

    //-----------------------------------------------------------------------------------------------default

    /** zookeeper会话超时 */
    public static final int DEFAULT_SESSION_TIMEOUT = 30000;

    private ZKConstants() {
    }
}
