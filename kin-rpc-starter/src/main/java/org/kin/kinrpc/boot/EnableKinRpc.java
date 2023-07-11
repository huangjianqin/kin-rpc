package org.kin.kinrpc.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 同时启用kinrpc service和reference
 *
 * @author huangjianqin
 * @date 2020/12/15
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(KinRpcMarkerConfiguration.class)
public @interface EnableKinRpc {
}
