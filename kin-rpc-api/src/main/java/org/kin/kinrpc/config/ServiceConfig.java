package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.kinrpc.bootstrap.ServiceBootstrap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * 服务配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class ServiceConfig<T> extends AbstractInterfaceConfig<T, ServiceConfig<T>> {
    /** 传输层配置 */
    private final List<ServerConfig> servers = new ArrayList<>();
    /** 服务方法执行线程池 */
    private ExecutorConfig executor;
    /** 服务实例 */
    private T service;
    /** bootstrap 类型 */
    private String bootstrap = "kinrpc";

    private transient ServiceBootstrap<T> serviceBootstrap;

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
    @SuppressWarnings("unchecked")
    public synchronized void export() {
        if (Objects.isNull(serviceBootstrap)) {
            serviceBootstrap = ExtensionLoader.getExtension(ServiceBootstrap.class, bootstrap, this);
        }

        serviceBootstrap.export();
    }

    /**
     * 服务下线
     */
    public synchronized void unExport() {
        if (Objects.isNull(serviceBootstrap)) {
            return;
        }

        serviceBootstrap.unExport();
    }

    //setter && getter
    public List<ServerConfig> getServers() {
        return servers;
    }

    public ServiceConfig<T> servers(ServerConfig... servers) {
        return servers(Arrays.asList(servers));
    }

    public ServiceConfig<T> servers(List<ServerConfig> servers) {
        this.servers.addAll(servers);
        return this;
    }

    public ExecutorConfig getExecutor() {
        return executor;
    }

    public ServiceConfig<T> executor(ExecutorConfig executor) {
        this.executor = executor;
        return this;
    }

    public T getService() {
        return service;
    }

    public ServiceConfig<T> service(T service) {
        this.service = service;
        return this;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public ServiceConfig<T> bootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }
}