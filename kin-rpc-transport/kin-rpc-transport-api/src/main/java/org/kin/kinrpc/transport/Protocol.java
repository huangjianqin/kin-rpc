package org.kin.kinrpc.transport;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ProviderInvoker;

/**
 * 传输层协议, 目前仅仅支持kinrpc(自己实现, 基于netty), grpc, http, protobuf, thrift
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
    <T> Exporter<T> export(ProviderInvoker<T> invoker) throws Throwable;

    /**
     * reference service
     *
     * @param type service interface
     * @param <T>  service类型
     * @return protocol wrappered invoker
     */
    <T> AsyncInvoker<T> reference(Url url) throws Throwable;

    /**
     * 释放占用资源
     */
    void destroy();
}
