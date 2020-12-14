package org.kin.kinrpc.spring;

/**
 * 传输协议附加配置
 *
 * @author huangjianqin
 * @date 2020/12/12
 */
public @interface Attachment {
    String key();

    String value();
}
