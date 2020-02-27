package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019-09-10
 */
public enum InvokeType {
    /**
     * java自带代理
     */
    JAVA,
    /**
     * JAVASSIST字节码代理
     */
    JAVASSIST,
    ;
}
