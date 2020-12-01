package org.kin.kinrpc.transport.grpc;

import io.grpc.BindableService;
import io.grpc.HandlerRegistry;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * todo
 *
 * @author huangjianqin
 * @date 2020/12/1
 */
public class GrpcHandlerRegistry extends HandlerRegistry {
    /** 服务定义 */
    private final Map<String, ServerServiceDefinition> services = new ConcurrentHashMap<>();
    /** 服务方法定义 */
    private final Map<String, ServerMethodDefinition<?, ?>> methods = new ConcurrentHashMap<>();

    /**
     * Returns the service definitions in this registry.
     */
    @Override
    public List<ServerServiceDefinition> getServices() {
        return Collections.unmodifiableList(new ArrayList<>(services.values()));
    }

    /**
     * 寻找服务方法定义
     */
    @Nullable
    @Override
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        return methods.get(methodName);
    }

    /**
     * 添加服务
     */
    void addService(BindableService bindableService, String key) {
        ServerServiceDefinition service = bindableService.bindService();
        services.put(key, service);
        for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
            methods.put(method.getMethodDescriptor().getFullMethodName(), method);
        }
    }

    /**
     * 移除服务
     */
    void removeService(String serviceKey) {
        ServerServiceDefinition service = services.remove(serviceKey);
        for (ServerMethodDefinition<?, ?> method : service.getMethods()) {
            methods.remove(method.getMethodDescriptor().getFullMethodName(), method);
        }
    }
}