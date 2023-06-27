package org.kin.kinrpc.config;

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
    /** executor name */
    private String name;
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

    /**
     * 复用executor
     * 注意该executor必须在服务export前进行注册
     *
     * @param name executor name
     * @return {@link ExecutorConfig}实例
     */
    public static ExecutorConfig fromExists(String name) {
        return new ExecutorConfig().name(name);
    }

    public static ExecutorConfig create(String executor) {
        return new ExecutorConfig().executor(executor);
    }

    public static ExecutorConfig create(ExecutorType executorType) {
        return create(executorType.getName());
    }

    public static ExecutorConfig create(String name, String executor) {
        return create(executor).name(name);
    }

    public static ExecutorConfig create(String name, ExecutorType executorType) {
        return create(executorType).name(name);
    }

    public static ExecutorConfig cache() {
        return create(ExecutorType.CACHE);
    }

    public static ExecutorConfig cache(String name) {
        return create(name, ExecutorType.CACHE);
    }

    public static ExecutorConfig direct() {
        return create(ExecutorType.DIRECT);
    }

    public static ExecutorConfig direct(String name) {
        return create(name, ExecutorType.DIRECT);
    }

    public static ExecutorConfig eager() {
        return create(ExecutorType.EAGER);
    }

    public static ExecutorConfig eager(String name) {
        return create(name, ExecutorType.EAGER);
    }

    public static ExecutorConfig fix() {
        return create(ExecutorType.FIX);
    }

    public static ExecutorConfig fix(String name) {
        return create(name, ExecutorType.FIX);
    }

    private ExecutorConfig() {
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public ExecutorConfig name(String name) {
        this.name = name;
        return this;
    }

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
