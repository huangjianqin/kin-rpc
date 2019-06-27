package org.kin.kinrpc.transport;

import io.netty.channel.Channel;

/**
 * Created by huangjianqin on 2019/5/30.
 */
@FunctionalInterface
public interface SessionBuilder {
    /**
     * 在channel线程调用, 最好内部捕获异常, 不然会导致channel因异常关闭
     */
    AbstractSession create(Channel channel);
}
