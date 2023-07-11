package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * kinrpc服务标识
 *
 * @author huangjianqin
 * @date 2020/12/6
 * @see KinRpcServiceBeanProcessor
 * @see org.kin.kinrpc.config.ServiceConfig
 * @see org.kin.kinrpc.bootstrap.KinRpcBootstrap#service(ServiceConfig)
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Service
public @interface KinRpcService {
    /** registry config id, also registry config bean name */
    String[] registries() default {};

    /** 服务所属组 */
    String group() default DefaultConfig.DEFAULT_GROUP;

    /** 服务名 */
    String serviceName() default "";

    /** 版本号 */
    String version() default DefaultConfig.DEFAULT_VERSION;

    /** 序列化方式 */
    String serialization() default DefaultConfig.DEFAULT_SERIALIZATION;

    /** filter bean name */
    String[] filter() default {};


    /** 服务接口 */
    Class<?> interfaceClass();

    /** 是否仅在jvm内发布服务 */
    boolean jvm() default false;

    /** server config id, also server config bean name */
    String[] servers() default {};

    /** service executor config id, also executor config bean name */
    String executor() default "";

    /** 权重 */
    int weight() default 1;

    /** rpc请求认证token */
    String token() default "";

    /** 延迟发布时间, 毫秒 */
    long delay() default 0L;
}
