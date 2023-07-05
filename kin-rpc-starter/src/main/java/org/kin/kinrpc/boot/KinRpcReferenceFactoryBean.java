package org.kin.kinrpc.boot;

import com.google.common.base.Preconditions;
import org.kin.framework.log.LoggerOprs;
import org.kin.kinrpc.conf.ReferenceConfig;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;

import java.util.Objects;

/**
 * 构建{@link KinRpcReference}注解在interface的服务reference实例
 *
 * @author huangjianqin
 * @date 2021/1/17
 */
final class KinRpcReferenceFactoryBean<T> extends AbstractFactoryBean<T> implements ApplicationContextAware, EnvironmentAware, LoggerOprs {
    private AbstractEnvironment environment;
    private ApplicationContext applicationContext;
    /** 服务接口 */
    private final Class<T> interfaceClass;
    /** 服务接口上的@KinRpcReference注解 */
    private final KinRpcReference anno;
    /** KinRpcReference注解数据 */
    private final AnnotationAttributes annoAttrs;
    /** 服务引用配置 */
    private ReferenceConfig<T> referenceConfig;

    KinRpcReferenceFactoryBean(Class<T> interfaceClass) {
        this.interfaceClass = interfaceClass;
        anno = AnnotationUtils.findAnnotation(interfaceClass, KinRpcReference.class);

        Preconditions.checkNotNull(anno, String.format("interface '%s' doesn't have annotation '%s'", interfaceClass, KinRpcReference.class));

        annoAttrs = AnnotationUtils.getAnnotationAttributes(null, anno, false, false);
    }

    @Override
    protected T createInstance() throws Exception {
        if (Objects.isNull(referenceConfig)) {
            referenceConfig = KinRpcAnnoUtils.convert2ReferenceCfg(
                    applicationContext,
                    annoAttrs,
                    interfaceClass,
                    null,
                    environment.getProperty("spring.application.name", "kinrpc"));
        }
        return referenceConfig.get();
    }

    @Override
    protected void destroyInstance(T instance) throws Exception {
        super.destroyInstance(instance);

        if (Objects.nonNull(referenceConfig)) {
            referenceConfig.disable();
        }
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }

    /**
     * 单例
     */
    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = (AbstractEnvironment) environment;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
