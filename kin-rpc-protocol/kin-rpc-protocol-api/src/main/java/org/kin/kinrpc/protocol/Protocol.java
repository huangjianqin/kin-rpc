package org.kin.kinrpc.protocol;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.Exporter;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.SslConfig;

/**
 * 传输层协议, 目前仅仅支持kinrpc(自研, 基于netty), grpc, protobuf
 *
 * @author huangjianqin
 * @date 2020/11/3
 */
@SPI("protocol")
public interface Protocol {
    /**
     * export service
     *
     * @param rpcService   service
     * @param serverConfig server config
     * @param <T>          service interface
     * @return {@link Exporter}实例
     */
    <T> Exporter<T> export(RpcService<T> rpcService, ServerConfig serverConfig);

    /**
     * reference service
     *
     * @param instance service instance
     * @apiNote sslConfig   ssl config
     * @param <T>      service interface
     * @return {@link  ReferenceInvoker}实例
     */
    <T> ReferenceInvoker<T> refer(ServiceInstance instance, SslConfig sslConfig);

    /**
     * 释放占用资源
     */
    void destroy();
}
