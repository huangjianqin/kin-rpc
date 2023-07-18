package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Filter;
import org.kin.kinrpc.IllegalConfigException;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/6/16
 */
public abstract class AbstractInterfaceConfig<IC extends AbstractInterfaceConfig<IC>> extends AttachableConfig {
    /** 应用配置 */
    private ApplicationConfig app;
    /** 注册中心配置 */
    private List<RegistryConfig> registries = new ArrayList<>();
    /** 服务所属组 */
    private String group;
    /** 版本号 */
    private String version;
    /**
     * 序列化方式
     * 对于reference端而言, 相当于兜底, 如果{@link org.kin.kinrpc.ServiceInstance}没有序列化方式, 则使用reference配置的序列化方式
     */
    private String serialization;
    /** filter list */
    private transient final List<Filter> filters = new ArrayList<>();

    protected AbstractInterfaceConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(Objects.nonNull(app), "app config must be not null");
        check(StringUtils.isNotBlank(group), "group be not blank");
        check(StringUtils.isNotBlank(version), "version be not blank");
        check(StringUtils.isNotBlank(serialization), "serialization type be not blank");
        if (isJvmBootstrap()) {
            return;
        }

        for (RegistryConfig registryConfig : registries) {
            registryConfig.checkValid();
        }
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.nonNull(app)) {
            app.initDefaultConfig();
        }

        for (RegistryConfig registry : registries) {
            registry.initDefaultConfig();
        }

        if (Objects.isNull(group)) {
            group = DefaultConfig.DEFAULT_GROUP;
        }

        if (Objects.isNull(version)) {
            version = DefaultConfig.DEFAULT_VERSION;
        }

        if (Objects.isNull(serialization)) {
            serialization = DefaultConfig.DEFAULT_SERIALIZATION;
        }
    }

    @SuppressWarnings("unchecked")
    protected final IC castThis() {
        return (IC) this;
    }

    /**
     * 判断是否是jvm bootstrap
     *
     * @return true表示是jvm bootstrap
     */
    protected boolean isJvmBootstrap() {
        return false;
    }

    //setter && getter
    public ApplicationConfig getApp() {
        return app;
    }

    public IC app(ApplicationConfig app) {
        this.app = app;
        return castThis();
    }

    public List<RegistryConfig> getRegistries() {
        return registries;
    }

    public void setRegistries(List<RegistryConfig> registries) {
        if (Objects.isNull(registries)) {
            throw new IllegalConfigException("registries can not be null");
        }
        this.registries = registries;
    }

    public IC registry(RegistryConfig registry) {
        return registries(Collections.singletonList(registry));
    }

    public IC registries(RegistryConfig... registries) {
        return registries(Arrays.asList(registries));
    }

    public IC registries(Collection<RegistryConfig> registries) {
        this.registries.addAll(registries);
        return castThis();
    }

    public String getGroup() {
        return group;
    }

    public IC group(String group) {
        this.group = group;
        return castThis();
    }

    public String getVersion() {
        return version;
    }

    public IC version(String version) {
        this.version = version;
        return castThis();
    }

    public String getSerialization() {
        return serialization;
    }

    public IC serialization(String serialization) {
        this.serialization = serialization;
        return castThis();
    }

    public IC serialization(SerializationType serializationType) {
        return serialization(serializationType.getName());
    }

    public List<Filter> getFilters() {
        return filters;
    }

    public IC filter(Filter filter) {
        return filters(Collections.singletonList(filter));
    }

    public IC filters(Filter... filters) {
        return filters(Arrays.asList(filters));
    }

    public IC filters(List<Filter> filters) {
        this.filters.addAll(filters);
        return castThis();
    }

    //----------------------
    public AbstractInterfaceConfig<IC> setGroup(String group) {
        this.group = group;
        return this;
    }

    public AbstractInterfaceConfig<IC> setVersion(String version) {
        this.version = version;
        return this;
    }

    public AbstractInterfaceConfig<IC> setSerialization(String serialization) {
        this.serialization = serialization;
        return this;
    }

    @Override
    public String toString() {
        return "app=" + app +
                ", registries=" + registries +
                ", serialization='" + serialization + '\'';
    }
}
