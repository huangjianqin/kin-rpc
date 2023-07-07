package org.kin.kinrpc.config;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.utils.ObjectUtils;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/7/7
 */
public abstract class AbstractServiceConfig<ASC extends AbstractServiceConfig<ASC>>
        extends AbstractInterfaceConfig<ASC> {
    /** 传输层配置 */
    private final List<ServerConfig> servers = new ArrayList<>();
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
    }

    //setter && getter
    public List<ServerConfig> getServers() {
        return servers;
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

    public ASC servers(List<ServerConfig> servers) {
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

    public int getWeight() {
        return weight;
    }

    public ASC weight(int weight) {
        this.weight = weight;
        return castThis();
    }

    public long getDelay() {
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

    @Override
    public String toString() {
        return super.toString() +
                ObjectUtils.toStringIfPredicate(CollectionUtils.isNonEmpty(servers), ", servers=" + servers) +
                ObjectUtils.toStringIfNonNull(executor, ", executor=" + executor) +
                ", weight=" + weight +
                ", bootstrap='" + bootstrap + '\'' +
                ", delay=" + delay +
                ", token=" + token;
    }
}
