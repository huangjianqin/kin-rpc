package org.kin.kinrpc.transport.grpc;

import io.grpc.HandlerRegistry;
import io.grpc.MethodDescriptor;
import io.grpc.ServerMethodDefinition;
import io.grpc.ServerServiceDefinition;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class DefaultHandlerRegistry extends HandlerRegistry {
    /** key -> 服务唯一id, value -> service definition */
    private final Map<Integer, ServerServiceDefinition> services = new CopyOnWriteMap<>();

    @Override
    public List<ServerServiceDefinition> getServices() {
        return Collections.unmodifiableList(new ArrayList<>(services.values()));
    }

    @Override
    @Nullable
    public ServerMethodDefinition<?, ?> lookupMethod(String methodName, @Nullable String authority) {
        String serviceName = MethodDescriptor.extractFullServiceName(methodName);
        if (StringUtils.isBlank(serviceName)) {
            return null;
        }

        //转换成serviceId
        int idx = serviceName.indexOf(GrpcConstants.SERVICE_PREFIX);
        int serviceId = Integer.parseInt(serviceName.substring(idx + GrpcConstants.SERVICE_PREFIX.length()));

        ServerServiceDefinition service = services.get(serviceId);
        if (service == null) {
            return null;
        }
        return service.getMethod(methodName);
    }


    /**
     * 注册服务
     *
     * @param serviceId 服务唯一id
     */
    public void addService(int serviceId, ServerServiceDefinition service) {
        services.put(serviceId, service);
    }

    /**
     * 注销服务
     *
     * @param serviceId 服务唯一id
     */
    public void removeService(int serviceId) {
        services.remove(serviceId);
    }
}
