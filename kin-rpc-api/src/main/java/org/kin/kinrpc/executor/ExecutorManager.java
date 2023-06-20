package org.kin.kinrpc.executor;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.RpcException;
import org.kin.kinrpc.config.ExecutorConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 服务调用线程池管理
 * todo map key为string的是否可优化性能
 *
 * @author huangjianqin
 * @date 2023/3/1
 */
public class ExecutorManager {
    /**
     * 服务调用线程池
     * key -> executor name
     */
    private static final Map<String, ServiceExecutor> executorMap = new HashMap<>();

    static {
        JvmCloseCleaner.instance().add(() -> {
            for (ServiceExecutor executor : executorMap.values()) {
                executor.shutdown();
            }
        });
    }

    /**
     * 获取服务线程池
     *
     * @param gsv    服务唯一标识
     * @param config 服务调用线程池配置
     * @return 服务线程池
     */
    public static synchronized ServiceExecutor getOrCreateExecutor(String gsv, ExecutorConfig config) {
        String name = config.getName();
        if (StringUtils.isNotBlank(name)) {
            //复用已注册的线程池
            ServiceExecutor serviceExecutor = executorMap.get(name);
            if (Objects.isNull(serviceExecutor)) {
                throw new RpcException(String.format("can not find executor with name '%s'", name));
            }

            return serviceExecutor;
        } else {
            //默认executor name
            //创建并注册线程池
            return createExecutor(config.name(gsv + "-default"));
        }
    }

    /**
     * 注册服务调用线程池
     *
     * @param name     服务调用线程池唯一标识
     * @param executor 服务调用线程池
     */
    public static synchronized void registerExecutor(String name, ServiceExecutor executor) {
        if (executorMap.containsKey(name)) {
            throw new RpcException(String.format("executor name with '%s' has registered", name));
        }

        executorMap.put(name, executor);
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
    private static ServiceExecutor createExecutor(ExecutorConfig config) {
        String name = config.getName();
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("executor name must be not blank");
        }

        if (executorMap.containsKey(name)) {
            throw new RpcException(String.format("executor name with '%s' has registered", name));
        }

        ExecutorFactory executorFactory = ExtensionLoader.getExtension(ExecutorFactory.class, config.getExecutor());
        if (Objects.isNull(executorFactory)) {
            throw new RpcException(String.format("can not find executor factory for type '%s'", config.getExecutor()));
        }

        ServiceExecutor serviceExecutor = executorFactory.create(config);
        executorMap.put(name, serviceExecutor);
        return serviceExecutor;
    }

    /**
     * 移除服务调用线程池
     *
     * @param name 服务调用线程池唯一标识
     */
    public static synchronized void removeExecutor(String name) {
        ServiceExecutor serviceExecutor = executorMap.get(name);
        if (Objects.isNull(serviceExecutor)) {
            return;
        }

        serviceExecutor.shutdown();
    }
}
