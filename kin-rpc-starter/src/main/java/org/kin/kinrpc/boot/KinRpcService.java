package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.lang.annotation.*;
import java.util.Map;

/**
 * kinrpc服务标识
 * 两种用法(推荐使用第一种):
 * 1. 注解在服务实现类上
 * <pre>
 * &#64;KinRpcService(interfaceClass = HelloService.class)
 * public class HelloServiceImpl implements HelloService {
 *     ...
 * }
 * </pre>
 * <p>
 * 2. 在configuration class中注解在构造service bean方法上, 类似{@link Bean}
 * <pre>
 * &#64;Configuration
 * class ServiceConfiguration {
 *
 *     &#64;KinRpcService(interfaceClass = HelloService.class)
 *     public HelloService helloService(){
 *         return new HelloServiceImpl();
 *     }
 * }
 * </pre>
 * <p>
 * !!!注意修改属性名, 记得也要同步修改{@link KinRpcServiceBeanProcessor#addServiceConfig(Map, Object)}中使用到的属性名
 *
 * @author huangjianqin
 * @date 2020/12/6
 * @see KinRpcServiceBeanProcessor
 * @see org.kin.kinrpc.config.ServiceConfig
 * @see org.kin.kinrpc.bootstrap.KinRpcBootstrap#service(ServiceConfig)
 */
@Documented
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Service
@Bean
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
    String[] filters() default {};


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

    /** 标识是否异步export */
    boolean exportAsync() default false;
}
