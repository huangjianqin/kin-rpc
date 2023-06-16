package org.kin.kinrpc.rpc.common.config1;

import org.kin.kinrpc.rpc.common.config.ApplicationConfig;

import java.util.*;

/**
 * todo interceptor配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public abstract class AbstractInterfaceConfig<T, IC extends AbstractInterfaceConfig<T, IC>> extends AbstractConfig {
    /** 应用配置 */
    private ApplicationConfig applicationConfig;
    /** 注册中心配置 */
    private final List<RegistryConfig> registryConfigs = new ArrayList<>();
    /** 服务方法配置 */
    private final Map<String, MethodConfig> methodConfigMap = new HashMap<>();
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务所属组 */
    private String group = "";
    /** 服务名 */
    private String serviceName;
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 默认序列化方式 */
    private String serialization;

    protected AbstractInterfaceConfig() {
    }

    @SuppressWarnings("unchecked")
    protected IC castThis() {
        return (IC) this;
    }

    //setter && getter
    public ApplicationConfig getApplicationConfig() {
        return applicationConfig;
    }

    public IC app(ApplicationConfig applicationConfig) {
        this.applicationConfig = applicationConfig;
        return castThis();
    }

    public List<RegistryConfig> getRegistryConfigs() {
        return registryConfigs;
    }

    public IC registries(RegistryConfig... registryConfigs) {
        return registries(Arrays.asList(registryConfigs));
    }

    public IC registries(List<RegistryConfig> registryConfigs) {
        this.registryConfigs.addAll(registryConfigs);
        return castThis();
    }

    public Map<String, MethodConfig> getMethodConfigMap() {
        return methodConfigMap;
    }

    public IC method(String method, MethodConfig methodConfig) {
        this.methodConfigMap.put(method, methodConfig);
        return castThis();
    }

    public IC methods(Map<String, MethodConfig> methodConfigMap) {
        this.methodConfigMap.putAll(methodConfigMap);
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
}
