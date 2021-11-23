package org.kin.kinrpc.spring;

import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.loadbalance.RoundRobinLoadBalance;
import org.kin.kinrpc.cluster.router.NoneRouter;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;

import java.lang.annotation.*;

/**
 * 仅仅允许注解在interface上
 * 配置的含义请看{@link org.kin.kinrpc.config.ReferenceConfig}
 * 两种用法:
 * 1. 注解在Field上. 当多个service需要使用同一reference时, 将{@link KinRpcReference}, {@link RedisRegistry}和{@link ZookeeperRegistry}注解复制到其他用到Field上即可.
 * 建议所有注解复制一遍, 因为无法保证注入顺序, 如果beanname一样, 但其余一些配置不一样, 只会使用最先注入的配置而创建service reference
 * 此类beanName=Reference${appName}${serviceName}${version}.
 * 2. 注解在服务接口上. 可以通过@Autowired, 在需要的services注入服务引用. 此方法不太友好, 需要复制接口代码
 * 3. 在@Configuration配置类中使用{@link org.kin.kinrpc.config.ReferenceConfig}构建Reference并注册Bean
 *
 * @author huangjianqin
 * @date 2020/12/6
 * @see KinRpcReferenceFieldProcessor
 * @see KinRpcReferenceRegistrar
 * @see EnableKinRpcReferencePostProcessor
 * @see KinRpcReferenceScanner
 * @see KinRpcReferenceFactoryBean
 */
@Documented
@Target({ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface KinRpcReference {
    /** reference bean name */
    String beanName() default "";

    String appName() default "";

    /** jvm进程内寻找服务 */
    boolean jvm() default false;

    String[] urls() default {};

    String serviceName() default "";

    String version() default "0.1.0.0";

    int retryTimes() default 0;

    long retryInterval() default Constants.RETRY_INTERVAL;

    //loadBalance
    Class<? extends LoadBalance> loadBalanceClass() default RoundRobinLoadBalance.class;

    String loadBalance() default "";

    //router
    Class<? extends Router> routerClass() default NoneRouter.class;

    String router() default "";


    boolean byteCodeEnhance() default true;

    int tps() default Constants.PROVIDER_DEFAULT_TPS;

    boolean async() default false;

    Class<? extends Notifier<?>>[] notifiers() default {};

    boolean useGeneric() default false;

    long callTimeout() default Constants.RPC_CALL_TIMEOUT;

    boolean ssl() default false;

    Attachment[] attachment() default {};
}
