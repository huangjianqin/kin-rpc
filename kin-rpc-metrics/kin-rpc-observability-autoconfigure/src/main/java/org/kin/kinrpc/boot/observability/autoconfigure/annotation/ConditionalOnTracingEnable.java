package org.kin.kinrpc.boot.observability.autoconfigure.annotation;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.lang.annotation.*;

/**
 * @author huangjianqin
 * @date 2023/8/26
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@ConditionalOnProperty(prefix = "kinrpc.tracing", name = "enabled")
public @interface ConditionalOnTracingEnable {
}
