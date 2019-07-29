package org.kin.kinrpc.transport.domain;

import org.kin.kinrpc.transport.ProtocolHandler;
import org.kin.kinrpc.transport.SessionBuilder;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public abstract class TransportOption<T extends TransportOption<?>> {
    protected ProtocolHandler protocolHandler;
    protected SessionBuilder sessionBuilder;

    public T protocolHandler(ProtocolHandler protocolHandler) {
        this.protocolHandler = protocolHandler;
        return (T) this;
    }

    public T sessionBuilder(SessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
        return (T) this;
    }

    //getter
    public ProtocolHandler getProtocolHandler() {
        return protocolHandler;
    }

    public SessionBuilder getSessionBuilder() {
        return sessionBuilder;
    }
}
