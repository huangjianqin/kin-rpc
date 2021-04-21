package org.kin.kinrpc.spring;

import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.rpc.Notifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.InjectionMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
final class KinRpcAnnoUtils {
    private static final Logger log = LoggerFactory.getLogger(KinRpcAnnoUtils.class);

    private KinRpcAnnoUtils() {
    }

    /**
     * 将zookeeper注册中心注解转换成注册中心配置
     */
    private static ZookeeperRegistryConfig convert2RegistryCfg(@Nonnull ZookeeperRegistry zookeeperRegistryAnno) {
        return ZookeeperRegistryConfig.create(zookeeperRegistryAnno.address())
                .sessionTimeout(zookeeperRegistryAnno.sessionTimeout()).build();
    }

    /**
     * 将redis注册中心注解转换成注册中心配置
     */
    private static RedisRegistryConfig convert2RegistryCfg(@Nonnull RedisRegistry redisRegistryAnno) {
        return RedisRegistryConfig.create(redisRegistryAnno.address())
                .watchInterval(redisRegistryAnno.watchInterval()).build();
    }

    /**
     * 根据{@link Attachment}解析出注册中心配置
     */
    public static Map<String, Object> parseAttachment(Attachment[] attachmentAnnos) {
        if (CollectionUtils.isEmpty(attachmentAnnos)) {
            return Collections.emptyMap();
        }

        Map<String, Object> attachment = new HashMap<>();
        for (Attachment attachmentAnno : attachmentAnnos) {
            attachment.put(attachmentAnno.key(), attachmentAnno.value());
        }

        return attachment;
    }

    /**
     * {@link KinRpcService}和{@link KinRpcReference}都使用
     * 寻找注册中心配置
     */
    public static AbstractRegistryConfig parseRegistryConfig(Class<?> beanClass, ApplicationContext applicationContext) {
        //todo 目前支持配置一个注册中心, 如果使用了多个, 则取第一个
        ZookeeperRegistry zookeeperRegistryAnno = AnnotationUtils.getAnnotation(beanClass, ZookeeperRegistry.class);
        if (Objects.nonNull(zookeeperRegistryAnno)) {
            return convert2RegistryCfg(zookeeperRegistryAnno);
        }

        RedisRegistry redisRegistryAnno = AnnotationUtils.getAnnotation(beanClass, RedisRegistry.class);
        if (Objects.nonNull(redisRegistryAnno)) {
            return convert2RegistryCfg(redisRegistryAnno);
        }

        //2. 都没有
        //寻找应用统一注册中心配置, springboot配置或者@Bean注入
        if (Objects.nonNull(applicationContext)) {
            Map<String, AbstractRegistryConfig> registryConfigMap =
                    applicationContext.getBeansOfType(AbstractRegistryConfig.class);
            if (CollectionUtils.isNonEmpty(registryConfigMap)) {
                return registryConfigMap.entrySet().iterator().next().getValue();
            }
        }
        return null;
    }

    /**
     * {@link KinRpcReference}使用
     * 寻找注册中心配置
     */
    public static AbstractRegistryConfig parseRegistryConfig(Field field, ApplicationContext applicationContext) {
        //1. field上是否有注册中心注解
        //todo 目前支持配置一个注册中心, 如果使用了多个, 则取第一个
        ZookeeperRegistry zookeeperRegistryAnno = AnnotationUtils.getAnnotation(field, ZookeeperRegistry.class);
        if (Objects.nonNull(zookeeperRegistryAnno)) {
            return convert2RegistryCfg(zookeeperRegistryAnno);
        }

        RedisRegistry redisRegistryAnno = AnnotationUtils.getAnnotation(field, RedisRegistry.class);
        if (Objects.nonNull(redisRegistryAnno)) {
            return convert2RegistryCfg(redisRegistryAnno);
        }

        //2. 查看当前是否有注册中心注解
        return parseRegistryConfig(field.getDeclaringClass(), applicationContext);
    }

    /**
     * @return 注解的appName
     */
    public static String getAppName(AnnotationAttributes attributes, String defaultAppName) {
        String appName = attributes.getString("appName");
        if (StringUtils.isBlank(appName)) {
            //如果没有配置appName, 则尝试用spring application name, 最后用默认
            appName = defaultAppName;
            if (StringUtils.isBlank(appName)) {
                //用默认
                appName = "kinrpc";
            }
        }

        return appName;
    }

    /**
     * @return 注解的服务名
     */
    public static String getServiceName(AnnotationAttributes attributes, Class<?> interfaceClass) {
        String serviceName = attributes.getString("serviceName");
        if (StringUtils.isBlank(serviceName)) {
            //如果没有配置serviceName, 则采用接口java package path命名
            serviceName = interfaceClass.getCanonicalName();
        }

        return serviceName;
    }

    /**
     * 根据服务引用注解和注册中心注解, 解析出服务引用配置
     *
     * @param applicationContext spring容器上下文
     * @param annoAttrs          KinRpcReference注解属性map
     * @param interfaceClass     服务接口
     * @param injectedElement    注入元素, 即注入信息, 如果KinRpcReference注解用于Field时, 则需要传入注入数据
     * @param defaultAppName     默认的spring application name
     * @return 服务引用配置
     * @see ReferenceConfig
     * @see RedisRegistryConfig
     * @see ZookeeperRegistryConfig
     * @see RedisRegistry
     * @see ZookeeperRegistry
     * @see KinRpcReference
     * @see RedisRegistry
     */
    @SuppressWarnings("unchecked")
    public static <T> ReferenceConfig<T> convert2ReferenceCfg(ApplicationContext applicationContext,
                                                              AnnotationAttributes annoAttrs,
                                                              Class<T> interfaceClass,
                                                              InjectionMetadata.InjectedElement injectedElement,
                                                              String defaultAppName) {
        String appName = getAppName(annoAttrs, defaultAppName);

        String serviceName = getServiceName(annoAttrs, interfaceClass);

        //new服务引用配置
        ReferenceConfig<T> referenceConfig = References.reference(interfaceClass)
                .app(appName)
                .service(serviceName)
                .version(annoAttrs.getString("version"))
                .retry(annoAttrs.getNumber("retryTimes"))
                .retryTimeout(annoAttrs.getNumber("retryInterval"))
                .tps(annoAttrs.getNumber("tps"))
                .notify((Class<? extends Notifier<?>>[]) annoAttrs.getClassArray("notifiers"))
                .callTimeout(annoAttrs.getNumber("callTimeout"));

        //jvm
        boolean jvm = annoAttrs.getBoolean("jvm");
        if (jvm) {
            referenceConfig.jvm();
        } else {
            //url
            String[] urls = annoAttrs.getStringArray("urls");
            if (CollectionUtils.isNonEmpty(urls)) {
                //直连
                referenceConfig.urls(urls);
            } else {
                //寻找注册中心
                AbstractRegistryConfig registryConfig;
                if (Objects.isNull(injectedElement)) {
                    registryConfig = KinRpcAnnoUtils.parseRegistryConfig(interfaceClass, applicationContext);
                } else {
                    registryConfig = KinRpcAnnoUtils.parseRegistryConfig((Field) injectedElement.getMember(), applicationContext);
                }
                if (Objects.nonNull(registryConfig)) {
                    referenceConfig.registry(registryConfig);
                } else {
                    log.warn("reference '{}' does not config a registry, check it is all right", serviceName);
                }
            }
        }

        //loadBalance
        //1. 默认先找通过字符串定义的loadBalance
        String loadBalance = annoAttrs.getString("loadBalance");
        if (StringUtils.isNotBlank(loadBalance)) {
            referenceConfig.loadbalance(loadBalance);
        } else {
            //2. loadBalance class
            referenceConfig.loadbalance(annoAttrs.getClass("loadBalanceClass"));
        }

        //router
        //1. 默认先找通过字符串定义的router
        String router = annoAttrs.getString("router");
        if (StringUtils.isNotBlank(router)) {
            referenceConfig.router(router);
        } else {
            //2. router class
            referenceConfig.router(annoAttrs.getClass("routerClass"));
        }

        //byteCodeEnhance
        if (annoAttrs.getBoolean("byteCodeEnhance")) {
            referenceConfig.javassistProxy();
        } else {
            referenceConfig.javaProxy();
        }

        //async
        if (annoAttrs.getBoolean("async")) {
            referenceConfig.async();
        }

        //useGeneric
        if (annoAttrs.getBoolean("useGeneric")) {
            referenceConfig.useGeneric();
        }

        //ssl
        if (annoAttrs.getBoolean("ssl")) {
            referenceConfig.enableSsl();
        }

        //attachment
        Map<String, Object> attachment = KinRpcAnnoUtils.parseAttachment(annoAttrs.getAnnotationArray("attachment", Attachment.class));
        if (CollectionUtils.isNonEmpty(attachment)) {
            referenceConfig.attach(attachment);
        }

        return referenceConfig;
    }
}
