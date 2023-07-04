package org.kin.kinrpc.config;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.bootstrap.ServiceBootstrap;
import org.kin.kinrpc.utils.ObjectUtils;

import java.lang.reflect.Method;
import java.util.*;

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
        //检查接口方法是否有重载
        Set<String> availableMethodNames = new HashSet<>();
        for (Method method : getInterfaceClass().getDeclaredMethods()) {
            String methodName = method.getName();
            if (!availableMethodNames.add(methodName)) {
                throw new IllegalConfigException(String.format("service interface method name '%s' conflict, does not support method overload now", methodName));
            }
        }
        if (Objects.nonNull(executor)) {
            executor.checkValid();
        }
        check(Objects.nonNull(instance), "service instance must be not null");
        check(StringUtils.isNotBlank(bootstrap), "boostrap type must be not null");

        if (isJvmBootstrap()) {
            return;
        }

        check(servers.size() > 0, "server config must be config at least one");
        for (ServerConfig serverConfig : servers) {
            serverConfig.checkValid();
        }
    }

    @Override
    protected boolean isJvmBootstrap() {
        return BootstrapType.JVM.getName().equalsIgnoreCase(bootstrap);
    }

    /**
     * 发布服务
     */
    @SuppressWarnings("unchecked")
    public synchronized ServiceConfig<T> export() {
        setUpDefaultConfig();
        checkValid();

        if (Objects.isNull(serviceBootstrap)) {
            serviceBootstrap = ExtensionLoader.getExtension(ServiceBootstrap.class, bootstrap, this);
        }

        serviceBootstrap.export();
        return this;
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

    public ServiceConfig<T> jvm() {
        return bootstrap(BootstrapType.JVM);
    }

    public ServiceConfig<T> server(ServerConfig server) {
        return servers(Collections.singletonList(server));
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

    public ServiceConfig<T> bootstrap(BootstrapType bootstrapType) {
        this.bootstrap = bootstrapType.getName();
        return this;
    }

    public int getWeight() {
        return weight;
    }

    public ServiceConfig<T> weight(int weight) {
        this.weight = weight;
        return this;
    }

    @Override
    public String toString() {
        return "ServiceConfig{" +
                super.toString() +
                ObjectUtils.toStringIfPredicate(CollectionUtils.isNonEmpty(servers), ", servers=" + servers) +
                ObjectUtils.toStringIfNonNull(executor, ", executor=" + executor) +
                ", weight=" + weight +
                ", instance=" + instance +
                ", bootstrap='" + bootstrap + '\'' +
                '}';
    }
}
