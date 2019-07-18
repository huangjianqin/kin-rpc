package org.kin.kinrpc.transport.netty.listener;

import io.netty.channel.Channel;

/**
 * @author huangjianqin
 * @date 2019/6/27
 */

public interface ChannelIdleListener {
    /**
     * 在channel线程调用
     */
    void allIdle(Channel channel);

    /**
     * @return 获取allIdle timeout时间, 秒数
     */
    int allIdleTime();

    /**
     * 在channel线程调用
     */
    void readIdle(Channel channel);

    /**
     * @return readIdle timeout时间, 秒数
     */
    int readIdleTime();

    /**
     * 在channel线程调用
     */
    void writeIdel(Channel channel);

    /**
     * @return writeIdel timeout时间, 秒数
     */
    int writeIdelTime();
}
