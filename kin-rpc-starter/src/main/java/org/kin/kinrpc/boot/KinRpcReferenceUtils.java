package org.kin.kinrpc.boot;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.IllegalConfigException;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Arrays;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/10
 */
public final class KinRpcReferenceUtils {
    private KinRpcReferenceUtils() {
    }

    /**
     * {@code referenceAnno}转换成{@link ReferenceConfig}实例
     *
     * @param referenceAnno kinrpc reference注解
     * @return {@link ReferenceConfig}实例
     */
    public static <T> ReferenceConfig<T> toReferenceConfig(ApplicationContext applicationContext,
                                                           Class<T> interfaceClass,
                                                           KinRpcReference referenceAnno) {
        Map<String, Object> annoAttrs = AnnotationUtils.getAnnotationAttributes(referenceAnno, false, true);
        return toReferenceConfig(applicationContext, interfaceClass, annoAttrs);
    }

    /**
     * {@code referenceAnnoAttrs}转换成{@link ReferenceConfig}实例
     *
     * @param referenceAnnoAttrs {@link KinRpcReference}  注解属性
     * @return {@link ReferenceConfig}实例
     */
    @SuppressWarnings("unchecked")
    public static <T> ReferenceConfig<T> toReferenceConfig(ApplicationContext applicationContext,
                                                           Class<T> interfaceClass,
                                                           Map<String, Object> referenceAnnoAttrs) {
        ReferenceConfig<T> referenceConfig = ReferenceConfig.create(interfaceClass)
                .group((String) referenceAnnoAttrs.get("group"))
                .version((String) referenceAnnoAttrs.get("version"))
                .serialization((String) referenceAnnoAttrs.get("serialization"))
                .cluster((String) referenceAnnoAttrs.get("cluster"))
                .loadBalance((String) referenceAnnoAttrs.get("loadBalance"))
                .router((String) referenceAnnoAttrs.get("router"))
                .generic((Boolean) referenceAnnoAttrs.get("generic"))
                .rpcTimeout((Integer) referenceAnnoAttrs.get("rpcTimeout"))
                .retries((Integer) referenceAnnoAttrs.get("retries"))
                .async((Boolean) referenceAnnoAttrs.get("async"))
                .sticky((Boolean) referenceAnnoAttrs.get("sticky"));

        String serviceName = (String) referenceAnnoAttrs.get("serviceName");
        if (StringUtils.isBlank(serviceName)) {
            serviceName = interfaceClass.getName();
        }
        referenceConfig.serviceName(serviceName);

        String[] registries = (String[]) referenceAnnoAttrs.get("registries");
        if (CollectionUtils.isNonEmpty(registries)) {
            for (String registryConfigId : registries) {
                referenceConfig.registries(RegistryConfig.fromId(registryConfigId));
            }
        }

        String[] filters = (String[]) referenceAnnoAttrs.get("filters");
        if (CollectionUtils.isNonEmpty(filters)) {
            FilterUtils.addFilters(applicationContext, () -> Arrays.asList(filters), referenceConfig::filter);
        }

        if ((Boolean) referenceAnnoAttrs.get("jvm")) {
            referenceConfig.jvm();
        }

        AnnotationAttributes[] handlerAnnoAttrsList = (AnnotationAttributes[]) referenceAnnoAttrs.get("handlers");
        if (CollectionUtils.isNonEmpty(handlerAnnoAttrsList)) {
            for (AnnotationAttributes handlerAnnoAttrs : handlerAnnoAttrsList) {
                String name = (String) handlerAnnoAttrs.get("name");
                if (StringUtils.isBlank(name)) {
                    throw new IllegalConfigException(String.format("handler name is blank, KinRpcReference=%s", referenceAnnoAttrs));
                }
                referenceConfig.handler(MethodConfig.create(name)
                        .timeout((Integer) handlerAnnoAttrs.get("rpcTimeout"))
                        .retries((Integer) handlerAnnoAttrs.get("retries"))
                        .async((Boolean) handlerAnnoAttrs.get("async"))
                        .sticky((Boolean) handlerAnnoAttrs.get("sticky")));
            }
        }

        return referenceConfig;
    }

    /**
     * 使用spring aop创建延迟创建reference代理实例
     *
     * @param referenceConfig reference config
     * @return reference代理实例
     */
    @SuppressWarnings("unchecked")
    public static <T> T createLazyProxy(ReferenceConfig<T> referenceConfig,
                                        ClassLoader beanClassLoader) {
        Class<?> interfaceClass = referenceConfig.getInterfaceClass();

        ProxyFactory proxyFactory = new ProxyFactory();
        proxyFactory.setTargetSource(new KinRpcReferenceLazyCreationTargetSource(referenceConfig));
        proxyFactory.addInterface(interfaceClass);

        return (T) proxyFactory.getProxy(beanClassLoader);
    }
}
