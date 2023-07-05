package org.kin.kinrpc.boot;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 同时启用kinrpc service和reference
 *
 * @author huangjianqin
 * @date 2020/12/15
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@EnableKinRpcService
@EnableKinRpcReference
public @interface EnableKinRpc {
    /**
     * 指定扫描定义有@KinRpcReference的服务接口的classpath
     */
    @AliasFor(annotation = EnableKinRpcReference.class, attribute = "scanBasePackages")
    String[] scanBasePackages() default {};
}
