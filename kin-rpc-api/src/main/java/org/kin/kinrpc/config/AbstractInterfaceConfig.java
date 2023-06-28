package org.kin.kinrpc.config;

import org.kin.kinrpc.Interceptor;
import org.kin.kinrpc.utils.GsvUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * todo interceptor配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public abstract class AbstractInterfaceConfig<T, IC extends AbstractInterfaceConfig<T, IC>> extends AbstractConfig {
    /** 应用配置 */
    private ApplicationConfig app;
    /** 注册中心配置 */
    private final List<RegistryConfig> registries = new ArrayList<>();
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务所属组 */
    private String group = "";
    /** 服务名 */
    private String serviceName;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 默认序列化方式 */
    private String serialization = SerializationType.JSON.getName();
    // TODO: 2023/6/20 spring通过bean name查询 interceptor来添加
    /** 服务调用拦截器列表 */
    private List<Interceptor> interceptors;

    protected AbstractInterfaceConfig() {
    }

    @SuppressWarnings("unchecked")
    protected IC castThis() {
        return (IC) this;
    }

    /**
     * todo 是否缓存
     * 获取服务唯一标识
     *
     * @return 服务唯一标识
     */
    public String service() {
        return GsvUtils.service(group, serviceName, version);
    }

    /**
     * 获取服务唯一标识
     *
     * @return 服务唯一标识
     */
    public int serviceId() {
        return GsvUtils.serviceId(group, serviceName, version);
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

    public IC interfaceClass(Class<T> interfaceClass) {
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

    public AbstractInterfaceConfig<T, IC> interceptors(Interceptor... interceptors) {
        return interceptors(Arrays.asList(interceptors));
    }

    public AbstractInterfaceConfig<T, IC> interceptors(List<Interceptor> interceptors) {
        this.interceptors.addAll(interceptors);
        return this;
    }
}
