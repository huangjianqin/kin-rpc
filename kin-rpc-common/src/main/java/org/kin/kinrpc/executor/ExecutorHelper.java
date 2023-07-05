package org.kin.kinrpc.executor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ExecutorConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * executor helper, 管理executor生命周期, 也允许user注册自定义executor
 *
 * @author huangjianqin
 * @date 2023/3/1
 */
public class ExecutorHelper {
    /**
     * 服务调用线程池
     * key -> executor name
     */
    private static final Map<String, ManagedExecutor> EXECUTOR_MAP = new HashMap<>();

    static {
        JvmCloseCleaner.instance().add(() -> {
            for (ManagedExecutor executor : EXECUTOR_MAP.values()) {
                executor.shutdown();
            }
        });
    }

    /**
     * 获取服务线程池
     *
     * @param defaultName 默认线程池唯一标识
     * @param config      服务调用线程池配置
     * @return 服务线程池
     */
    public static synchronized ManagedExecutor getOrCreateExecutor(ExecutorConfig config, String defaultName) {
        String name = config.getName();
        if (StringUtils.isNotBlank(name)) {
            //复用已注册的线程池
            ManagedExecutor executor = EXECUTOR_MAP.get(name);
            if (Objects.isNull(executor)) {
                throw new IllegalArgumentException(new ExecutorNotFoundException(name));
            }

            return executor;
        } else {
            //默认executor name
            //创建并注册线程池
            return createExecutor(config.name(defaultName));
        }
    }

    /**
     * 注册服务调用线程池
     *
     * @param name     服务调用线程池唯一标识
     * @param executor 服务调用线程池
     */
    public static synchronized void registerExecutor(String name, ManagedExecutor executor) {
        if (EXECUTOR_MAP.containsKey(name)) {
            throw new IllegalArgumentException(String.format("executor name with '%s' has registered", name));
        }

        EXECUTOR_MAP.put(name, wrapExecutor(name, executor));
    }

    /**
     * 注册服务调用线程池
     *
     * @param config 服务调用线程池配置
     */
    public static synchronized void registerExecutor(ExecutorConfig config) {
        createExecutor(config);
    }

    /**
     * 创建并注册服务调用线程池
     *
     * @param config 服务调用线程池配置
     */
    private static ManagedExecutor createExecutor(ExecutorConfig config) {
        String name = config.getName();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("executor name must be not blank");
        }

        if (EXECUTOR_MAP.containsKey(name)) {
            throw new IllegalArgumentException(String.format("executor name with '%s' has registered", name));
        }

        ExecutorFactory executorFactory = ExtensionLoader.getExtension(ExecutorFactory.class, config.getType());
        if (Objects.isNull(executorFactory)) {
            throw new IllegalArgumentException(String.format("can not find executor factory for type '%s'", config.getType()));
        }

        ManagedExecutor executor = wrapExecutor(name, executorFactory.create(config));
        EXECUTOR_MAP.put(name, executor);
        return executor;
    }

    /**
     * 对{@link ManagedExecutor#shutdown()}进行封装, 使用{@link #removeExecutor(String)}来完成 executor shutdown
     * 以防executor user shutdown executor, 但没有从{@link #EXECUTOR_MAP}移除, 导致无用对象无法gc, 一直占用内存
     *
     * @param name     executor name
     * @param executor executor
     * @return wrapped executor instance
     */
    private static ManagedExecutor wrapExecutor(String name, ManagedExecutor executor) {
        return new ManagedExecutor() {
            @Override
            public void execute(Runnable command) {
                executor.execute(command);
            }

            @Override
            public void shutdown() {
                EXECUTOR_MAP.remove(name);
                executor.shutdown();
            }
        };
    }

    /**
     * 移除服务调用线程池
     *
     * @param name 服务调用线程池唯一标识
     */
    public static synchronized void removeExecutor(String name) {
        ManagedExecutor executor = EXECUTOR_MAP.remove(name);
        if (Objects.isNull(executor)) {
            return;
        }

        executor.shutdown();
    }
}
