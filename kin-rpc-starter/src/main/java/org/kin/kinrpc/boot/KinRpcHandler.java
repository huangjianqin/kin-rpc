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
}
