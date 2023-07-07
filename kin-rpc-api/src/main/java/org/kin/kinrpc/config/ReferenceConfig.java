package org.kin.kinrpc.config;

import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.bootstrap.ReferenceBootstrap;
import org.kin.kinrpc.utils.GsvUtils;

import java.lang.reflect.Method;
import java.util.*;

/**
 * 服务引用配置
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public final class ReferenceConfig<T> extends AbstractReferenceConfig<ReferenceConfig<T>> {
    /** 接口 */
    private Class<T> interfaceClass;
    /** 服务名 */
    private String serviceName;
    /** 服务方法配置 */
    private final List<MethodConfig> methods = new ArrayList<>();

    //----------------------------------------------------------------动态变量, lazy init
    /** 返回服务唯一标识 */
    private transient String service;
    /** 返回服务唯一id */
    private transient int serviceId;
    private transient ReferenceBootstrap<T> referenceBootstrap;

    public static <T> ReferenceConfig<T> create(Class<T> interfaceClass) {
        return new ReferenceConfig<T>().interfaceClass(interfaceClass);
    }

    private ReferenceConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(Objects.nonNull(interfaceClass), "interface class be not null");
        if (isGeneric() && !GenericService.class.equals(getInterfaceClass())) {
            //开启泛化, 服务接口必须为GenericService
            throw new IllegalConfigException(String.format("open generic, interface class must be org.kin.kinrpc.GenericService, but actually is %s", getInterfaceClass().getName()));
        }
        check(StringUtils.isNotBlank(serviceName), "service name be not blank");

        Set<String> availableMethodNames = new HashSet<>();
        for (Method method : getInterfaceClass().getDeclaredMethods()) {
            boolean notExists = availableMethodNames.add(method.getName());
            if (!isGeneric() && !notExists) {
                throw new IllegalConfigException(String.format("service interface method name '%s' conflict, does not support method overload now", method.getName()));
            }
        }

        if (!isGeneric()) {
            for (MethodConfig methodConfig : methods) {
                methodConfig.checkValid();

                //检查方法名是否合法
                String name = methodConfig.getName();
                if (!availableMethodNames.contains(name)) {
                    throw new IllegalConfigException(String.format("method '%s' is not found in interface '%s'", name, getInterfaceClass().getName()));
                }
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

        for (MethodConfig method : methods) {
            method.initDefaultConfig();
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
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    @SuppressWarnings("unchecked")
    public synchronized T refer() {
        initDefaultConfig();
        checkValid();

        if (Objects.isNull(referenceBootstrap)) {
            referenceBootstrap = ExtensionLoader.getExtension(ReferenceBootstrap.class, getBootstrap(), this);
        }

        return referenceBootstrap.refer();
    }

    /**
     * 取消服务引用
     */
    public synchronized void unRefer() {
        if (Objects.isNull(referenceBootstrap)) {
            return;
        }

        referenceBootstrap.unRefer();
    }

    //setter && getter
    public Class<T> getInterfaceClass() {
        return interfaceClass;
    }

    protected ReferenceConfig<T> interfaceClass(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        return castThis();
    }

    public String getServiceName() {
        return serviceName;
    }

    public ReferenceConfig<T> serviceName(String serviceName) {
        this.serviceName = serviceName;
        return castThis();
    }

    public List<MethodConfig> getMethods() {
        return methods;
    }

    public ReferenceConfig<T> method(MethodConfig method) {
        this.methods.add(method);
        return this;
    }

    public ReferenceConfig<T> methods(MethodConfig... methods) {
        return methods(Arrays.asList(methods));
    }

    public ReferenceConfig<T> methods(Collection<MethodConfig> methods) {
        this.methods.addAll(methods);
        return this;
    }

    @Override
    public String toString() {
        return "ReferenceConfig{" +
                super.toString() +
                ", interfaceClass=" + interfaceClass +
                ", service='" + service + '\'' +
                ", serviceId=" + serviceId +
                ", methods=" + methods +
                '}';
    }
}
