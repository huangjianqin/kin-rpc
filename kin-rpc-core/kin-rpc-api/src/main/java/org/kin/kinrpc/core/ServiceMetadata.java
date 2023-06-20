package org.kin.kinrpc.core;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.transport.ExecutorSelector;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 服务元数据
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class ServiceMetadata {
    private static final Logger log = LoggerFactory.getLogger(ServiceMetadata.class);

    /** 服务方法元数据 */
    private Map<String, MethodMetadata> methodMetadataMap;
    /** 服务invoker */
    private final Invoker<?> invoker;
    /** 服务调用线程池 */
    private final ExecutorSelector executorSelector;

    public ServiceMetadata(ServiceConfig<?> config,
                           Invoker<?> invoker,
                           ExecutorSelector executorSelector) {
        Class<?> interfaceClass = config.getInterfaceClass();
        Method[] declaredMethods = interfaceClass.getDeclaredMethods();

        Map<String, MethodMetadata> methodMetadataMap = new HashMap<>(declaredMethods.length);
        for (Method method : declaredMethods) {
            String uniqueName = method.getName();

            if (Object.class.equals(method.getDeclaringClass())) {
                //过滤Object定义的方法
                continue;
            }

            if (!RpcUtils.isRpcMethodValid(method)) {
                log.warn("service method '{}' is ignore, due to it is invalid", uniqueName);
                continue;
            }

            methodMetadataMap.put(uniqueName, new MethodMetadata(method));
        }
        this.methodMetadataMap = methodMetadataMap;
        this.invoker = invoker;
        this.executorSelector = executorSelector;
    }

    /**
     * 返回服务方法元数据
     *
     * @param methodName 服务方法名
     * @return 服务方法元数据
     */
    @Nullable
    public MethodMetadata getMethodMetadata(String methodName) {
        return methodMetadataMap.get(methodName);
    }

    public Invoker<?> getInvoker() {
        return invoker;
    }

    public ExecutorSelector getExecutorSelector() {
        return executorSelector;
    }
}
