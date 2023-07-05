package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Interceptor;
import org.kin.kinrpc.utils.GsvUtils;

import java.util.*;

/**
 * @author huangjianqin
 * @date 2023/6/16
 */
public abstract class AbstractInterfaceConfig<T, IC extends AbstractInterfaceConfig<T, IC>> extends AttachableConfig {
    /** 应用配置 */
    private ApplicationConfig app;
    /** 注册中心配置 */
    private final List<RegistryConfig> registries = new ArrayList<>();
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务所属组 */
    private String group = "kinrpc";
    /** 服务名 */
    private String serviceName;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 默认序列化方式 */
    private String serialization = SerializationType.JSON.getName();
    // TODO: 2023/6/20 spring通过bean name查询 interceptor来添加
    /** 服务调用拦截器列表 */
    private List<Interceptor> interceptors = new ArrayList<>();

    //----------------------------------------------------------------动态变量, lazy init
    /** 返回服务唯一标识 */
    private transient String service;
    /** 返回服务唯一id */
    private transient int serviceId;

    protected AbstractInterfaceConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        check(Objects.nonNull(app), "app config must be not null");
        check(Objects.nonNull(interfaceClass), "interface class be not null");
        check(StringUtils.isNotBlank(group), "group be not blank");
        check(StringUtils.isNotBlank(serviceName), "service name be not blank");
        check(StringUtils.isNotBlank(version), "version be not blank");
        check(StringUtils.isNotBlank(serialization), "serialization type be not blank");
        if (isJvmBootstrap()) {
            return;
        }

        for (RegistryConfig registryConfig : registries) {
            registryConfig.checkValid();
        }
    }

    /**
     * 缺省配置, 设置默认值
     */
    @Override
    protected void setUpDefaultConfig() {
        if (StringUtils.isBlank(getServiceName())) {
            serviceName(getInterfaceClass().getSimpleName());
        }
    }

    @SuppressWarnings("unchecked")
    protected IC castThis() {
        return (IC) this;
    }

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    public String getService() {
        if (Objects.isNull(service)) {
            service = GsvUtils.service(group, serviceName, version);
        }
        return service;
    }

    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id
     */
    public int getServiceId() {
        if (serviceId == 0) {
            serviceId = GsvUtils.serviceId(getService());
        }
        return serviceId;
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

    public IC registry(RegistryConfig registry) {
        return registries(Collections.singletonList(registry));
    }

    public IC registries(RegistryConfig... registries) {
        return registries(Arrays.asList(registries));
    }

    public IC registries(List<RegistryConfig> registries) {
        this.registries.addAll(registries);
        return castThis();
    }

    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    protected IC interfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return castThis();
    }

    public String getGroup() {
        return group;
    }

    public IC group(String group) {
        this.group = group;
        return castThis();
    }

    public String getServiceName() {
        return serviceName;
    }

    public IC serviceName(String serviceName) {
        this.serviceName = serviceName;
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

    public List<Interceptor> getInterceptors() {
        return interceptors;
    }

    public IC interceptor(Interceptor interceptor) {
        return interceptors(Collections.singletonList(interceptor));
    }

    public IC interceptors(Interceptor... interceptors) {
        return interceptors(Arrays.asList(interceptors));
    }

    public IC interceptors(List<Interceptor> interceptors) {
        this.interceptors.addAll(interceptors);
        return castThis();
    }

    @Override
    public String toString() {
        return "app=" + app +
                ", registries=" + registries +
                ", interfaceClass=" + interfaceClass +
                ", group='" + group + '\'' +
                ", serviceName='" + serviceName + '\'' +
                ", version='" + version + '\'' +
                ", serialization='" + serialization + '\'' +
                ", service='" + service + '\'' +
                ", serviceId=" + serviceId;
    }
}
