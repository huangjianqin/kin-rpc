package org.kin.kinrpc.spring;

import org.kin.kinrpc.cluster.LoadBalance;
import org.kin.kinrpc.cluster.Router;
import org.kin.kinrpc.cluster.loadbalance.RoundRobinLoadBalance;
import org.kin.kinrpc.cluster.router.NoneRouter;
import org.kin.kinrpc.rpc.Notifier;
import org.kin.kinrpc.rpc.common.Constants;

import java.lang.annotation.*;

/**
 * 配置的含义请看{@link org.kin.kinrpc.config.ReferenceConfig}
 *
 * @author huangjianqin
 * @date 2020/12/6
 */
@Documented
@Target(ElementType.FIELD)
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

    int rate() default Constants.PROVIDER_REQUEST_THRESHOLD;

    boolean async() default false;

    Class<? extends Notifier<?>>[] notifiers() default {};

    boolean useGeneric() default false;

    long callTimeout() default Constants.RPC_CALL_TIMEOUT;

    boolean ssl() default false;

    Attachment[] attachment() default {};
}
