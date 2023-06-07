package org.kin.kinrpc.transport;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * 抽象不同transport的操作
 * @author huangjianqin
 * @date 2023/6/2
 */
public interface ChannelContext {
    /**
     * write out message
     * @param msg  message
     */
    default void writeAndFlush(Object msg){
        writeAndFlush(msg, null);
    }

    /**
     * write out message
     * @param msg  message
     * @param listener  transport operation listener
     */
    void writeAndFlush(Object msg, @Nullable TransportOperationListener listener);

    /**
     * 返回client address
     * @return  client address
     */
    SocketAddress address();

    /**
     * 移除并返回request future
     * server侧永远返回null
     * @param requestId request id
     * @return  request future
     */
    @Nullable
    default CompletableFuture<Object> removeRequestFuture(long requestId){
        return null;
    }
}
