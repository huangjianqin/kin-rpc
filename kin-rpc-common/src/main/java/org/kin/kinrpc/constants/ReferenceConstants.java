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

    //---------------------------------------------------------------forking cluster
    /** forking cluster并发次数 */
    public static final String FORKING_FORKS_KEY = "cluster.forking.forks";

    //---------------------------------------------------------------broadcast cluster
    /** broadcast cluster服务调用失败的比例, 当达到这个比例后, 将不再发起调用, 直接抛出异常 */
    public static final String BROADCAST_FAIL_PERCENT_KEY = "cluster.broadcast.fal.percent";

    private ReferenceConstants() {
    }
}
