package org.kin.kinrpc.boot;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * kinrpc服务定义
 * service端:
 * 支持覆盖全局的server, registry url
 * 支持覆盖全局的excutor
 * 支持覆盖全局的group, version, serialization
 *
 * @author huangjianqin
 * @date 2020/12/6
 * @see org.kin.kinrpc.config.ServerConfig
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface KinRpcService {
    /**
     * 服务接口
     */
    Class<?> interfaceClass();

    String group() default "kinrpc";

    String serviceName() default "";

    String version() default "0.1.0.0";

    String serialization() default "0";

    String[] servers() default {};
    // TODO: 2023/7/5 ssl

    // TODO: 2023/7/5 executor
    int weight();

    String[] registries() default {};


}
