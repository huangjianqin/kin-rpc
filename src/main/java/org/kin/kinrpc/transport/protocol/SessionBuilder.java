package org.kin.kinrpc.transport.protocol;

import io.netty.channel.Channel;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public interface SessionBuilder {
    Session create(Channel channel);
}
