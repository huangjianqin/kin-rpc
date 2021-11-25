package org.kin.kinrpc.spring;

import org.kin.kinrpc.rpc.common.Constants;

import java.lang.annotation.*;

/**
 * kinrpc 注册中心注解, 不能单独使用, 需配合{@link KinRpcService}或者{@link KinRpcReference}
 * 配置详情, 请看{@link org.kin.kinrpc.config.RedisRegistryConfig}
 * <p>
 * todo 考虑使用redis publish-subscribe
 *
 * @author huangjianqin
 * @date 2020/12/13
 */
@Documented
@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface RedisRegistry {
    String address();

    long watchInterval() default Constants.WATCH_INTERVAL;
}
