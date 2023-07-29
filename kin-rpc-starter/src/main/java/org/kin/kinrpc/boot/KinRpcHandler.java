package org.kin.kinrpc.boot;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author huangjianqin
 * @date 2023/7/10
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface KinRpcHandler {
    /** 服务方法名 */
    String name();

    /** rpc call timeout(ms) */
    int rpcTimeout() default -1;

    /** 失败后重试次数 */
    int retries() default -1;

    /** 是否异步调用 */
    boolean async() default false;

    /** 是否服务调用粘黏 */
    boolean sticky() default false;

    /** 服务调用缓存类型 */
    String cache() default "";

    /** 是否开启服务方法调用参数校验 */
    boolean validation() default true;

    /** 自定义属性, {key1, value1, key2, value2.....} */
    String[] attachments() default {};
}
