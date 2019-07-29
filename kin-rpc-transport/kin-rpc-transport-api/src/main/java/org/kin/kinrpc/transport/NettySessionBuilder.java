package org.kin.kinrpc.transport;

import io.netty.channel.Channel;

/**
 * Created by huangjianqin on 2019/6/4.
 */
public abstract class NettySessionBuilder implements SessionBuilder<Channel> {
    /**
     * 对于netty而言, 在channel线程调用, 最好内部捕获异常, 不然会导致channel因异常关闭
     */
}
