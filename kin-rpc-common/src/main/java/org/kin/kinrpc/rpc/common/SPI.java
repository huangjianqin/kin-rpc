package org.kin.kinrpc.rpc.common;

import java.lang.annotation.*;

/**
 * 标识该接口支持SPI机制扩展
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Inherited
public @interface SPI {
}
