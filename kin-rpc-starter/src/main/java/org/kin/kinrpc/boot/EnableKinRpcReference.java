package org.kin.kinrpc.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用kinrpc reference
 *
 * @author huangjianqin
 * @date 2020/12/15
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({KinRpcReferenceFieldProcessor.class, KinRpcReferenceRegistrar.class})
public @interface EnableKinRpcReference {
    /**
     * 指定扫描定义有@KinRpcReference的服务接口的classpath
     */
    String[] scanBasePackages() default {};
}
