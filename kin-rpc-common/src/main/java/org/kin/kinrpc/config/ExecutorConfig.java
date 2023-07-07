package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.utils.ObjectUtils;

import java.util.Objects;

/**
 * 服务处理线程配置, 一般用于服务处理线程隔离
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ExecutorConfig extends AttachableConfig {
    /** executor name, 如果user不指定, 则跟service唯一标识和server唯一标识有关 */
    private String name;
    /** 服务处理线程类型 */
    private String type;
    /** core thread size, 默认为cpu处理器数量 */
    private Integer corePoolSize;
    /** max thread size, 默认为{@link Integer#MAX_VALUE} */
    private Integer maxPoolSize;
    /** thread pool keepAliveTime, default 60s */
    private Integer alive;
    /** thread pool's queue size */
    private Integer queueSize;

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
        return new ExecutorConfig().type(executor);
    }

    private static ExecutorConfig create(ExecutorType executorType) {
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

    @Override
    public void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(type), "executor type must be not blank");
        check(corePoolSize > 0, "executor corePoolSize must be greater than 0");
        check(maxPoolSize > 0, "executor maxPoolSize must be greater than 0");
        check(maxPoolSize >= corePoolSize, "executor maxPoolSize must be greater and equal than corePoolSize");
        check(alive > 0, "executor alive time must be greater than 0");
        check(queueSize > 0, "executor queue size must be greater than 0");
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.isNull(corePoolSize)) {
            corePoolSize = DefaultConfig.DEFAULT_EXECUTOR_CORE_POOL_SIZE;
        }

        if (Objects.isNull(maxPoolSize)) {
            maxPoolSize = DefaultConfig.DEFAULT_EXECUTOR_MAX_POOL_SIZE;
        }

        if (Objects.isNull(alive)) {
            alive = DefaultConfig.DEFAULT_EXECUTOR_ALIVE;
        }

        if (Objects.isNull(queueSize)) {
            queueSize = DefaultConfig.DEFAULT_EXECUTOR_QUEUE_SIZE;
        }
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public ExecutorConfig name(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public ExecutorConfig type(String type) {
        this.type = type;
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

    @Override
    public String toString() {
        return "ExecutorConfig{" +
                ObjectUtils.toStringIfPredicate(StringUtils.isNotBlank(name), "name='" + name + '\'') +
                ", type='" + type + '\'' +
                ", corePoolSize=" + corePoolSize +
                ", maxPoolSize=" + maxPoolSize +
                ", alive=" + alive +
                ", queueSize=" + queueSize +
                '}';
    }
}
