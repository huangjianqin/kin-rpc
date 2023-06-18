package org.kin.kinrpc.rpc.common.config;

import org.kin.framework.utils.SysUtils;

import java.util.concurrent.TimeUnit;

/**
 * 服务处理线程配置, 一般用于服务处理线程隔离
 * todo 是否可以考虑, 如果该executor队列满了, 是否允许在通用业务处理线程池完成服务调用
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ExecutorConfig extends AbstractConfig {
    /** 服务处理线程类型 */
    private String executor;
    /** core thread size, 默认为cpu处理器数量 */
    private int corePoolSize = SysUtils.getSuitableThreadNum();
    /** max thread size, 默认为{@link Integer#MAX_VALUE} */
    private int maxPoolSize = Integer.MAX_VALUE;
    /** thread pool keepAliveTime, default 60s */
    private int alive = (int) TimeUnit.SECONDS.toMillis(60);
    /** thread pool's queue size */
    private int queueSize = 1024;

    public static ExecutorConfig create(String executor) {
        return new ExecutorConfig().executor(executor);
    }

    public static ExecutorConfig create(ExecutorType executorType) {
        return create(executorType.getName());
    }


    private ExecutorConfig() {
    }

    //setter && getter
    public String getExecutor() {
        return executor;
    }

    public ExecutorConfig executor(String executor) {
        this.executor = executor;
        return this;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public ExecutorConfig corePoolSize(int corePoolSize) {
        this.corePoolSize = corePoolSize;
        return this;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public ExecutorConfig maxPoolSize(int maxPoolSize) {
        this.maxPoolSize = maxPoolSize;
        return this;
    }

    public int getAlive() {
        return alive;
    }

    public ExecutorConfig alive(int alive) {
        this.alive = alive;
        return this;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public ExecutorConfig queueSize(int queueSize) {
        this.queueSize = queueSize;
        return this;
    }
}
