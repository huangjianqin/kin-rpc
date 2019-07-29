package org.kin.kinrpc.transport.netty.impl;

import io.netty.channel.Channel;
import org.kin.kinrpc.transport.AbstractSession;
import org.kin.kinrpc.transport.SessionBuilder;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public class DefaultSessionBuilder implements SessionBuilder {
    @Override
    public AbstractSession create(Channel channel) {
        return new AbstractSession(channel, false) {
        };
    }
}
