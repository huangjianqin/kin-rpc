package org.kin.kinrpc.spring;

import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.config.AbstractRegistryConfig;
import org.kin.kinrpc.config.RedisRegistryConfig;
import org.kin.kinrpc.config.ZookeeperRegistryConfig;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020/12/12
 */
public class KinRpcAnnoUtils {
    private KinRpcAnnoUtils() {
    }

    /**
     * 将zookeeper注册中心注解转换成注册中心配置
     */
    private static ZookeeperRegistryConfig convert(@Nonnull ZookeeperRegistry zookeeperRegistryAnno) {
        return ZookeeperRegistryConfig.create(zookeeperRegistryAnno.address())
                .sessionTimeout(zookeeperRegistryAnno.sessionTimeout()).build();
    }

    /**
     * 将redis注册中心注解转换成注册中心配置
     */
    private static RedisRegistryConfig convert(@Nonnull RedisRegistry redisRegistryAnno) {
        return RedisRegistryConfig.create(redisRegistryAnno.address())
                .watchInterval(redisRegistryAnno.watchInterval()).build();
    }

    /**
     * {@link KinRpcService}和{@link KinRpcReference}都使用
     * 寻找注册中心配置
     */
    public static AbstractRegistryConfig parseRegistryConfig(Class<?> beanClass, ApplicationContext applicationContext) {
        //todo 目前支持配置一个注册中心, 如果使用了多个, 则取第一个
        for (Annotation declaredAnnotation : beanClass.getDeclaredAnnotations()) {
            //zookeeper
            if (ZookeeperRegistry.class.equals(declaredAnnotation.annotationType())) {
                return convert((ZookeeperRegistry) declaredAnnotation);
            }

            //redis
            if (RedisRegistry.class.equals(declaredAnnotation.annotationType())) {
                return convert((RedisRegistry) declaredAnnotation);
            }
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
     * {@link KinRpcReference}使用
     * 寻找注册中心配置
     */
    public static AbstractRegistryConfig parseRegistryConfig(Field field, ApplicationContext applicationContext) {
        //1. field上是否有注册中心注解
        //todo 目前支持配置一个注册中心, 如果使用了多个, 则取第一个
        for (Annotation declaredAnnotation : field.getDeclaredAnnotations()) {
            //zookeeper
            if (ZookeeperRegistry.class.equals(declaredAnnotation.annotationType())) {
                return convert((ZookeeperRegistry) declaredAnnotation);
            }

            //redis
            if (RedisRegistry.class.equals(declaredAnnotation.annotationType())) {
                return convert((RedisRegistry) declaredAnnotation);
            }
        }

        //2. 查看当前是否有注册中心注解
        return parseRegistryConfig(field.getDeclaringClass(), applicationContext);
    }
}
