package org.kin.kinrpc.rpc;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.common.Url;

/**
 * 传输层协议, 目前仅仅支持kinrpc(自研, 基于netty), grpc, protobuf
 *
 * @author huangjianqin
 * @date 2020/11/3
 */
@SPI(value = "kinrpc", alias = "protocol")
public interface Protocol {

    /**
     * export service
     *
     * @param invoker provider invoker
     * @param <T>     service类型
     * @return protocol wrappered invoker
     */
    <T> Exporter<T> export(Invoker<T> invoker) throws Throwable;

    /**
     * reference service
     *
     * @param type service interface
     * @param <T>  service类型
     * @return protocol wrappered invoker
     */
    <T> Invoker<T> refer(Url url) throws Throwable;

    /**
     * 释放占用资源
     */
    void destroy();
}
