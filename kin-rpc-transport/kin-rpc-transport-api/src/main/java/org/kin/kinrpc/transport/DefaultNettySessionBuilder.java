package org.kin.kinrpc.transport;

import io.netty.channel.Channel;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class DefaultNettySessionBuilder extends NettySessionBuilder {
    private static final SessionBuilder INSTANCE = new DefaultNettySessionBuilder();

    private DefaultNettySessionBuilder() {
    }

    public static SessionBuilder<Channel> instance(){
        return INSTANCE;
    }

    @Override
    public AbstractSession create(Channel channel) {
        return new AbstractSession(channel, false) {
        };
    }
}
