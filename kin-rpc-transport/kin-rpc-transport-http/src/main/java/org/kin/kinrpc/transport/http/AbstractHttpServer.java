package org.kin.kinrpc.transport.http;

import org.kin.kinrpc.rpc.common.Url;

import java.net.InetSocketAddress;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public abstract class AbstractHttpServer implements HttpServer {
    /** rpc url */
    protected final Url url;
    protected final HttpHandler handler;
    /** 标识server是否stopped */
    protected volatile boolean isStopped;

    public AbstractHttpServer(Url url, HttpHandler handler) {
        if (url == null) {
            throw new IllegalArgumentException("url == null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler == null");
        }
        this.url = url;
        this.handler = handler;
    }

    @Override
    public HttpHandler getHttpHandler() {
        return handler;
    }

    @Override
    public Url getUrl() {
        return url;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return InetSocketAddress.createUnresolved(url.getHost(), url.getPort());
    }

    @Override
    public void close() {
        isStopped = true;
    }

    @Override
    public boolean isAvailable() {
        return !isStopped;
    }
}
