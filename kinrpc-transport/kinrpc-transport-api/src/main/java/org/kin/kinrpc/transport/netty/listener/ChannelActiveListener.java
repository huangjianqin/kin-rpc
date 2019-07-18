package org.kin.kinrpc.transport.netty.listener;

import io.netty.channel.Channel;

/**
 * Created by huangjianqin on 2019/6/3.
 */
@FunctionalInterface
public interface ChannelActiveListener {
    /**
     * 在channel线程调用
     */
    void channelActive(Channel channel);
}
