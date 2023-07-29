package org.kin.kinrpc.constants;

/**
 * reference端常量
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ReferenceConstants {
    /** reference config provideBy字段分隔符 */
    public static final String PROVIDE_BY_SEPARATOR = ",";

    //---------------------------------------------------------------cluster
    //---------------------------------------------------------------failback cluster
    /** failback cluster重试次数 */
    public static final String FAILBACK_RETRIES_KEY = "cluster.failback.retries";
    /** failback cluster最大重试任务数量 */
    public static final String FAILBACK_RETRY_TASK_KEY = "cluster.failback.retryTask";

    private ReferenceConstants() {
    }
}
