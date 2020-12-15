package org.kin.kinrpc.spring;

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
@Import(KinRpcServiceProcessor.class)
public @interface EnableKinRpcService {
}
