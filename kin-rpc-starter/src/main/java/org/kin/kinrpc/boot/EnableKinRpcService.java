package org.kin.kinrpc.boot;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * 启用kinrpc service
 *
 * @author huangjianqin
 * @date 2020/12/15
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import(KinRpcServiceBeanProcessor.class)
public @interface EnableKinRpcService {
}
