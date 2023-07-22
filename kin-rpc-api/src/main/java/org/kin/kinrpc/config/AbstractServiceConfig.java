package org.kin.kinrpc.config;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.ServiceListener;
import org.kin.kinrpc.utils.ObjectUtils;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/7/7
 */
public abstract class AbstractServiceConfig<ASC extends AbstractServiceConfig<ASC>>
        extends AbstractInterfaceConfig<ASC> {
    /** 传输层配置 */
    private List<ServerConfig> servers = new ArrayList<>();
    /** 服务方法执行线程池 */
    private ExecutorConfig executor;
    /** 权重 */
    private Integer weight;
    /** bootstrap 类型 */
    private String bootstrap;
    /** 延迟发布时间, 毫秒 */
    private Long delay;
    /** 是否开启token校验 */
    private String token;
    /** 标识是否异步export */
    private Boolean exportAsync;
    /** 是否注册到注册中心 */
    private Boolean register;
    /** {@link ServiceListener}实例 */
    private final List<ServiceListener> serviceListeners = new ArrayList<>();

    @Override
    public void checkValid() {
        super.checkValid();
        if (Objects.nonNull(executor)) {
            executor.checkValid();
        }
        check(weight > 0, "service weight must be config at least one");
        check(StringUtils.isNotBlank(bootstrap), "service boostrap type must be not null");

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

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        for (ServerConfig server : servers) {
            server.initDefaultConfig();
        }

        if (Objects.nonNull(executor)) {
            executor.initDefaultConfig();
        }

        if (Objects.isNull(weight)) {
            weight = DefaultConfig.DEFAULT_SERVICE_WEIGHT;
        }

        if (Objects.isNull(bootstrap)) {
            bootstrap = DefaultConfig.DEFAULT_SERVICE_BOOTSTRAP;
        }

        if (Objects.isNull(delay)) {
            delay = DefaultConfig.DEFAULT_SERVICE_DELAY;
        }

        if (Objects.isNull(exportAsync)) {
            exportAsync = DefaultConfig.DEFAULT_SERVICE_EXPORT_ASYNC;
        }

        if (Objects.isNull(register)) {
            register = DefaultConfig.DEFAULT_SERVICE_REGISTER;
        }

        List<ServiceListener> serviceListeners = ExtensionLoader.getExtensions(ServiceListener.class);
        if (CollectionUtils.isNonEmpty(serviceListeners)) {
            this.serviceListeners.addAll(serviceListeners);
        }
    }

    //setter && getter
    public List<ServerConfig> getServers() {
        return servers;
    }

    public void setServers(List<ServerConfig> servers) {
        if (Objects.isNull(servers)) {
            throw new IllegalConfigException("servers can not be null");
        }
        this.servers = servers;
    }

    public ASC jvm() {
        return bootstrap(BootstrapType.JVM);
    }

    public ASC server(ServerConfig server) {
        return servers(Collections.singletonList(server));
    }

    public ASC servers(ServerConfig... servers) {
        return servers(Arrays.asList(servers));
    }

    public ASC servers(Collection<ServerConfig> servers) {
        this.servers.addAll(servers);
        return castThis();
    }

    public ExecutorConfig getExecutor() {
        return executor;
    }

    public ASC executor(ExecutorConfig executor) {
        this.executor = executor;
        return castThis();
    }

    public String getBootstrap() {
        return bootstrap;
    }

    public ASC bootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return castThis();
    }

    public ASC bootstrap(BootstrapType bootstrapType) {
        this.bootstrap = bootstrapType.getName();
        return castThis();
    }

    public Integer getWeight() {
        return weight;
    }

    public ASC weight(int weight) {
        this.weight = weight;
        return castThis();
    }

    public Long getDelay() {
        return delay;
    }

    public ASC delay(long delay) {
        this.delay = delay;
        return castThis();
    }

    public String getToken() {
        return token;
    }

    public ASC token(String token) {
        this.token = token;
        return castThis();
    }

    public Boolean isExportAsync() {
        return Objects.nonNull(exportAsync) ? exportAsync : false;
    }

    public ASC exportAsync(boolean exportAsync) {
        this.exportAsync = exportAsync;
        return castThis();
    }

    public ASC exportAsync() {
        return exportAsync(true);
    }

    public AbstractServiceConfig<ASC> register(Boolean register) {
        this.register = register;
        return this;
    }

    public AbstractServiceConfig<ASC> register() {
        return register(true);
    }

    public Boolean isRegister() {
        return Objects.nonNull(register) ? register : true;
    }

    public List<ServiceListener> getServiceListeners() {
        return serviceListeners;
    }

    public AbstractServiceConfig<ASC> serviceListeners(ServiceListener... serviceListeners) {
        return serviceListeners(Arrays.asList(serviceListeners));
    }

    public AbstractServiceConfig<ASC> serviceListeners(List<ServiceListener> serviceListeners) {
        this.serviceListeners.addAll(serviceListeners);
        return this;
    }

    //----------------------
    public AbstractServiceConfig<ASC> setExecutor(ExecutorConfig executor) {
        this.executor = executor;
        return this;
    }

    public AbstractServiceConfig<ASC> setWeight(Integer weight) {
        this.weight = weight;
        return this;
    }

    public AbstractServiceConfig<ASC> setBootstrap(String bootstrap) {
        this.bootstrap = bootstrap;
        return this;
    }

    public AbstractServiceConfig<ASC> setDelay(Long delay) {
        this.delay = delay;
        return this;
    }

    public AbstractServiceConfig<ASC> setToken(String token) {
        this.token = token;
        return this;
    }

    public void setExportAsync(Boolean exportAsync) {
        this.exportAsync = exportAsync;
    }

    public Boolean getRegister() {
        return register;
    }

    public void setRegister(Boolean register) {
        this.register = register;
    }

    @Override
    public String toString() {
        return super.toString() +
                ObjectUtils.toStringIfPredicate(CollectionUtils.isNonEmpty(servers), ", servers=" + servers) +
                ObjectUtils.toStringIfNonNull(executor, ", executor=" + executor) +
                ", weight=" + weight +
                ", bootstrap='" + bootstrap + '\'' +
                ", delay=" + delay +
                ", token=" + token +
                ", exportAsync=" + exportAsync;
    }
}
