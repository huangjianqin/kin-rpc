package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
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
    /** 权重 */
    private int weight;

    /** 服务实例 */
    private T instance;
    /** bootstrap 类型 */
    private String bootstrap = "kinrpc";

    //----------------------------------------------------------------动态变量, lazy init
    private transient ServiceBootstrap<T> serviceBootstrap;

    public static <T> ServiceConfig<T> create(Class<T> interfaceClass, T instance) {
        return new ServiceConfig<T>().interfaceClass(interfaceClass)
                .serviceName(interfaceClass.getCanonicalName())
                .instance(instance);

    }

    private ServiceConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        check(servers.size() > 0, "server config must be config at least one");
        for (ServerConfig serverConfig : servers) {
            serverConfig.checkValid();
        }
        if (Objects.nonNull(executor)) {
            executor.checkValid();
        }
        check(Objects.nonNull(instance), "service instance must be not null");
        check(StringUtils.isNotBlank(bootstrap), "boostrap type must be not null");
    }

    /**
     * 暴露服务
     */
    @SuppressWarnings("unchecked")
    public synchronized void export() {
        checkValid();

        if (Objects.isNull(serviceBootstrap)) {
            serviceBootstrap = ExtensionLoader.getExtension(ServiceBootstrap.class, bootstrap, this);
        }

        serviceBootstrap.export();
    }

    /**
     * 服务下线
     */
    public synchronized void unexport() {
        if (Objects.isNull(serviceBootstrap)) {
            return;
        }

        serviceBootstrap.unexport();
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

    public T getInstance() {
        return instance;
    }

    public ServiceConfig<T> instance(T instance) {
        this.instance = instance;
        return this;
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public ServiceConfig<T> bootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public int getWeight() {
        return weight;
    }

    public ServiceConfig<T> weight(int weight) {
        this.weight = weight;
        return this;
    }
}
