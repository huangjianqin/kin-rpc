package org.kin.kinrpc.transport.http.tomcat;


import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.transport.http.HttpBinder;
import org.kin.kinrpc.transport.http.HttpHandler;
import org.kin.kinrpc.transport.http.HttpServer;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class TomcatHttpBinder implements HttpBinder {
    @Override
    public HttpServer bind(Url url, HttpHandler handler) {
        return new TomcatHttpServer(url, handler);
    }
}
