package org.kin.kinrpc.transport.http;

import org.kin.kinrpc.rpc.common.Url;

import java.net.InetSocketAddress;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public interface HttpServer {
    /**
     * @return http handler.
     */
    HttpHandler getHttpHandler();

    /**
     * @return url
     */
    Url getUrl();

    /**
     * @return local address.
     */
    InetSocketAddress getLocalAddress();

    /**
     * close server.
     */
    void close();

    /**
     * @return 可用
     */
    boolean isAvailable();
}
