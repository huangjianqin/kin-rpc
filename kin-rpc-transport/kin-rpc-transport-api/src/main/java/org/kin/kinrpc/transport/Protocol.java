package org.kin.kinrpc.transport;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.Exporter;
import org.kin.kinrpc.rpc.invoker.Invoker;

/**
 * 传输层协议, 目前仅仅支持kinrpc(自己实现, 基于netty), grpc, http, protobuf, thrift
 *
 * @author huangjianqin
 * @date 2020/11/3
 */
@SPI(value = "kinrpc", key = "kinrpc.protocol")
public interface Protocol {
    /**
     * @return 获取该协议默认端口
     */
    int getDefaultPort();

    /**
     * export service
     *
     * @param invoker provider invoker
     * @param <T>     service类型
     * @return protocol wrappered invoker
     */
    <T> Exporter<T> export(Invoker<T> invoker);

    /**
     * reference service
     *
     * @param type service interface
     * @param <T>  service类型
     * @return protocol wrappered invoker
     */
    <T> Invoker<T> reference(Class<T> type);

    /**
     * 释放占用资源
     */
    void destroy();
}
