package org.kin.kinrpc.transport.protocol.impl;

import io.netty.channel.Channel;
import org.kin.kinrpc.transport.protocol.Session;
import org.kin.kinrpc.transport.protocol.SessionBuilder;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public class DefaultSessionBuilder implements SessionBuilder {
    @Override
    public Session create(Channel channel) {
        return new Session(channel, false) {
        };
    }
}
