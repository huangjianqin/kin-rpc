package org.kin.kinrpc.spring;

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
@EnableKinRpcService
@EnableKinRpcReference
public @interface EnableKinRpc {
}
