package org.kin.kinrpc.rpc.executor;

import org.kin.framework.Closeable;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.rpc.common.AttributeKey;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.config.ExecutorConfig;

import java.util.Map;
import java.util.concurrent.Executor;

/**
 * 管理服务线程池
 * @author huangjianqin
 * @date 2023/3/1
 */
public class ExecutorManager implements Closeable {
    /**
     * 服务executor
     * key -> gsv
     */
    private final Map<String, Executor> executorMap = new CopyOnWriteMap<>();

    /**
     * 获取服务线程池
     * @param url   服务url
     * @return  服务线程池
     */
    public Executor executor(Url url) {
        String executorKey = getExecu¬torKey(url);
        if(executorMap.containsKey(executorKey)){
            return executorMap.get(executorKey);
        }

        synchronized (url) {
            if(executorMap.containsKey(executorKey)){
                return executorMap.get(executorKey);
            }

            ExecutorConfig config = url.getAttribute(AttributeKey.EXECUTOR);
            ExecutorFactory executorFactory = ExtensionLoader.getExtension(ExecutorFactory.class, config.getExecutor().getName());
            Executor executor = executorFactory.executor(executorKey, url);
            executorMap.put(executorKey, executor);
            return executor;
        }
    }

    /**
     * 获取服务线程池唯一标识
     * @param url   服务url
     * @return  服务线程池唯一标识
     */
    private String getExecutorKey(Url url){
        return url.getServiceKey();
    }

    @Override
    public void close() {
        for (Executor executor : executorMap.values()) {

        }
    }
}
