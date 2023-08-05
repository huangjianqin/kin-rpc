package org.kin.kinrpc;

import java.lang.annotation.*;

/**
 * spi扩展类作用域
 * 用于决定扩展实例在哪个环境下生效
 *
 * @author huangjianqin
 * @date 2023/8/5
 */
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scope {
    String value();
}
