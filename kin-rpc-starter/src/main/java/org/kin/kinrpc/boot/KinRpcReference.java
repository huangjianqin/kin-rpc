package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.DefaultConfig;
import org.springframework.context.ApplicationContext;

import java.lang.annotation.*;
import java.util.Map;

/**
 * 配置的含义请看{@link org.kin.kinrpc.config.ReferenceConfig}
 * 两种用法(推荐使用第二种):
 * 1. 注解在字段上
 * <pre class="code">
 * public class Controller {
 *     &#64;KinRpcReference(serviceName = "hello",
 *         generic = true,
 *         handlers = {&#64;KinRpcHandler(name = "asyncFind"),
 *                 &#64;KinRpcHandler(name = "delayRandom", sticky = true, retries = 2),
 *                 &#64;KinRpcHandler(name = "asyncFind2", async = true),
 *         })
 *     private HelloService genericHelloService;
 * }
 * </pre>
 * 2. 在configuration class中使用{@link org.kin.kinrpc.config.ReferenceConfig}+{@link org.springframework.context.annotation.Bean}构建Reference并注册Bean,
 * 同时要求返回的Bean类型是{@link KinRpcReferenceBean}
 * <pre class="code">
 * &#64;Configuration
 * public class ReferenceConfiguration {
 *     &#64;Bean
 *     &#64;KinRpcReference(serviceName = "hello",
 *             handlers = {&#64;KinRpcHandler(name = "asyncFind"),
 *                     &#64;KinRpcHandler(name = "delayRandom", sticky = true, retries = 2),
 *                     &#64;KinRpcHandler(name = "asyncFind2", async = true),
 *             })
 *     public KinRpcReferenceBean&lt;HelloService&gt; helloService() {
 *         return new KinRpcReferenceBean&lt;&gt;(HelloService.class);
 *     }
 * }
 * </pre>
 * <p>
 * !!!注意修改属性名, 记得也要同步修改{@link KinRpcReferenceUtils#toReferenceConfig(ApplicationContext, Class, Map)}使用到的属性名
 *
 * @author huangjianqin
 * @date 2020/12/6
 * @see org.kin.kinrpc.config.ReferenceConfig
 * @see KinRpcReferenceBean
 * @see KinRpcReferenceFieldProcessor
 */
@Documented
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface KinRpcReference {
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


    /**
     * cluster类型
     *
     * @see org.kin.kinrpc.config.ClusterType
     */
    String cluster() default "failfast";

    /**
     * loadbalance类型
     *
     * @see org.kin.kinrpc.config.LoadBalanceType
     */
    String loadBalance() default "roundRobin";

    /**
     * router类型
     *
     * @see org.kin.kinrpc.config.RouterType
     */
    String router() default "none";

    /** 是否使用泛化调用 */
    boolean generic() default false;

    /** 是否仅在jvm内发布服务 */
    boolean jvm() default false;

    /** rpc call timeout(ms) */
    int rpcTimeout() default 3000;

    /** 失败后重试次数 */
    int retries() default 3;

    /** 是否异步调用 */
    boolean async() default false;

    /** 是否服务调用粘黏 */
    boolean sticky() default false;

    /** 服务方法配置 */
    KinRpcHandler[] handlers() default {};
}
