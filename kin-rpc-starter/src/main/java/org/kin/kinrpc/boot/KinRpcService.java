package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * kinrpc服务标识
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
    /** 服务接口 */
    Class<?> interfaceClass();

    /** 服务所属组 */
    String group() default DefaultConfig.DEFAULT_GROUP;

    /** 服务名 */
    String serviceName() default "";

    /** 版本号 */
    String version() default DefaultConfig.DEFAULT_VERSION;

    /** 序列化方式 */
    String serialization() default DefaultConfig.DEFAULT_SERIALIZATION;


    /** 传输层配置 */
    String[] servers() default {};

    /** service executor name, 即{@link ExecutorConfig#getName()} */
    String executor() default "";


    /** 权重 */
    int weight() default DefaultConfig.DEFAULT_SERVICE_WEIGHT;

    /** 注册中心name, 即{@link RegistryConfig#getName()} */
    String[] registries() default {};

    String token() default "";

    String[] filter() default {};
}
