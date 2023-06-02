package org.kin.kinrpc.transport.cmd;

import java.lang.annotation.*;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CommandCode {
    /** command code */
    short value();
}
