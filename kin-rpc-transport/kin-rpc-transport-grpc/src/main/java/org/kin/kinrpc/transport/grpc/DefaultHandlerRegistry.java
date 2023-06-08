package org.kin.kinrpc.transport.grpc;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import io.netty.buffer.ByteBuf;
import org.kin.framework.collection.CopyOnWriteMap;

import javax.annotation.Nullable;
import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class DefaultHandlerRegistry extends HandlerRegistry {
    /** key -> service name, value -> service definition */
    private final Map<String, ServerServiceDefinition> services = new CopyOnWriteMap<>();

    @Override
    public List<ServerServiceDefinition> getServices() {
        return Collections.unmodifiableList(new ArrayList<>(services.values()));
    }

    @Override
    @Nullable
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        String serviceName = MethodDescriptor.extractFullServiceName(methodName);
        if (serviceName == null) {
            return null;
        }
        ServerServiceDefinition service = services.get(serviceName);
        if (service == null) {
            return null;
        }
        return service.getMethod(methodName);
    }


    /**
     * 注册服务
     */
    public void addService(ServerServiceDefinition service) {
        services.put(service.getServiceDescriptor().getName(), service);
    }

    /**
     * 注销服务
     */
    public void removeService(String serviceName) {
        services.remove(serviceName);
    }
}
