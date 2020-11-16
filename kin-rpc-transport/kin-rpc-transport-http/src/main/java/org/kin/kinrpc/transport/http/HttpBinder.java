package org.kin.kinrpc.transport.http;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.rpc.common.Url;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
@SPI("tomcat")
public interface HttpBinder {
    /**
     * bind the server.
     *
     * @param url server url.
     * @return server.
     */
    HttpServer bind(Url url, HttpHandler handler);
}
