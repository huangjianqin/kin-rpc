package org.kin.kinrpc.spring;

/**
 * @author huangjianqin
 * @date 2020/12/6
 */
public @interface KinRpcReference {
    String appName() default "";

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
