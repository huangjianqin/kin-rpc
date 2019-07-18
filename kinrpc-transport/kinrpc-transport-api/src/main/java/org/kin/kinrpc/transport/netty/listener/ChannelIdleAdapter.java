package org.kin.kinrpc.transport.netty.listener;

import io.netty.channel.Channel;

/**
 * @author huangjianqin
 * @date 2019/6/27
 */
public class ChannelIdleAdapter implements ChannelIdleListener {
    @Override
    public void allIdle(Channel channel) {

    }

    @Override
    public int allIdleTime() {
        return 0;
    }

    @Override
    public void readIdle(Channel channel) {

    }

    @Override
    public int readIdleTime() {
        return 0;
    }

    @Override
    public void writeIdel(Channel channel) {

    }

    @Override
    public int writeIdelTime() {
        return 0;
    }
}
