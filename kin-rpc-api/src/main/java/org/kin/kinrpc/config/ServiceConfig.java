package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.bootstrap.ServiceBootstrap;
import org.kin.kinrpc.utils.GsvUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 服务配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ServiceConfig<T> extends AbstractServiceConfig<ServiceConfig<T>> {
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务名 */
    private String serviceName;
    /** 服务实例 */
    private T instance;

    //----------------------------------------------------------------动态变量, lazy init
    /** 返回服务唯一标识 */
    private transient String service;
    /** 返回服务唯一id */
    private transient int serviceId;
    private transient ServiceBootstrap<T> serviceBootstrap;

    public static <T> ServiceConfig<T> create(Class<T> interfaceClass, T instance) {
        return new ServiceConfig<T>().interfaceClass(interfaceClass)
                .serviceName(interfaceClass.getCanonicalName())
                .instance(instance);

    }

    private ServiceConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(Objects.nonNull(interfaceClass), "interface class be not null");
        check(StringUtils.isNotBlank(serviceName), "service name be not blank");
        check(Objects.nonNull(instance), "service instance must be not null");
        //检查接口方法是否有重载
        Set<String> availableMethodNames = new HashSet<>();
        for (Method method : getInterfaceClass().getDeclaredMethods()) {
            String methodName = method.getName();
            if (!availableMethodNames.add(methodName)) {
                throw new IllegalConfigException(String.format("service interface method name '%s' conflict, does not support method overload now", methodName));
            }
        }
    }

    /**
     * 缺省配置, 设置默认值
     */
    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (StringUtils.isBlank(getServiceName())) {
            serviceName(getInterfaceClass().getSimpleName());
        }
    }

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    public String getService() {
        if (Objects.isNull(service)) {
            service = GsvUtils.service(getGroup(), serviceName, getVersion());
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
     * 发布服务
     */
    @SuppressWarnings("unchecked")
    public synchronized ServiceConfig<T> export() {
        initDefaultConfig();
        checkValid();

        if (Objects.isNull(serviceBootstrap)) {
            serviceBootstrap = ExtensionLoader.getExtension(ServiceBootstrap.class, getBootstrap(), this);
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
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    protected ServiceConfig<T> interfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return castThis();
    }

    public String getServiceName() {
        return serviceName;
    }

    public ServiceConfig<T> serviceName(String serviceName) {
        this.serviceName = serviceName;
        return castThis();
    }

    public T getInstance() {
        return instance;
    }

    private ServiceConfig<T> instance(T instance) {
        this.instance = instance;
        return this;
    }

    @Override
    public String toString() {
        return "ServiceConfig{" +
                super.toString() +
                ", interfaceClass=" + interfaceClass +
                ", service='" + service + '\'' +
                ", serviceId=" + serviceId +
                ", instance=" + instance +
                '}';
    }
}
