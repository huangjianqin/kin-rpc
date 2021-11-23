package org.kin.kinrpc.transport.http;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.common.Url;

/**
 * 用于启动不同的http server
 *
 * @author huangjianqin
 * @date 2020/11/16
 */
@SPI(value = "tomcat", alias = "httpBinder")
public interface HttpBinder {
    /**
     * bind the server.
     *
     * @param url server url.
     * @return server.
     */
    HttpServer bind(Url url, HttpHandler handler);
}
