package org.kin.kinrpc.rpc.common.config1;

import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.common.config.AbstractRegistryConfig;
import org.kin.kinrpc.rpc.common.config.ApplicationConfig;
import org.kin.kinrpc.rpc.common.config.ServerConfig;
import org.kin.kinrpc.rpc.common.constants.Constants;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务配置
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ServiceConfig<T> extends AbstractInterfaceConfig<T, ServiceConfig<T>>{
    /** 传输层配置 */
    private final List<ServerConfig> serverConfigs = new ArrayList<>();
    /** 服务实例 */
    private T service;

    //------------------------------------------------------------------------------------------------
    /** 是否已暴露服务 */
    private AtomicBoolean exported;

    public static <T> ServiceConfig<T> create(Class<T> interfaceClass, T service) {
        return new ServiceConfig<T>().interfaceClass(interfaceClass)
                .serviceName(interfaceClass.getCanonicalName())
                .service(service);

    }

    private ServiceConfig() {
    }

    /**
     * 暴露服务
     */
    public void export(){
        // TODO: 2023/6/16
    }

    //setter && getter
    public List<ServerConfig> getServerConfigs() {
        return serverConfigs;
    }

    public ServiceConfig<T> servers(ServerConfig... serverConfigs) {
        return servers(Arrays.asList(serverConfigs));
    }

    public ServiceConfig<T> servers(List<ServerConfig> serverConfigs) {
        this.serverConfigs.addAll(serverConfigs);
        return this;
    }

    public T getService() {
        return service;
    }

    public ServiceConfig<T> service(T service) {
        this.service = service;
        return this;
    }
}
