package org.kin.kinrpc.config;

/**
 * 代理方式
 *
 * @author huangjianqin
 * @date 2019-09-10
 */
public enum ProxyType {
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
