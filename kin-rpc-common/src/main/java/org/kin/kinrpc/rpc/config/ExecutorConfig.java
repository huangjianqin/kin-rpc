package org.kin.kinrpc.rpc.config;

import org.kin.framework.utils.SysUtils;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/3/1
 */
public class ExecutorConfig {
    /** 服务隔离线程池类型 */
    private ExecutorType executor;
    /** core thread size */
    private int threadSize = SysUtils.getSuitableThreadNum();
    /** max thread size */
    private int maxThreadSize = Integer.MAX_VALUE;
    /** thread pool keepAliveTime, default unit milliseconds */
    private int alive = (int) TimeUnit.SECONDS.toMillis(60);
    /** thread pool's queue length */
    private int queueSize = 1024;

    //setter && getter
    public ExecutorType getExecutor() {
        return executor;
    }

    public ExecutorConfig executor(ExecutorType executor) {
        this.executor = executor;
        return this;
    }

    public int getThreadSize() {
        return threadSize;
    }

    public ExecutorConfig threadSize(int threadSize) {
        this.threadSize = threadSize;
        return this;
    }

    public int getMaxThreadSize() {
        return maxThreadSize;
    }

    public ExecutorConfig maxThreadSize(int maxThreadSize) {
        this.maxThreadSize = maxThreadSize;
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
