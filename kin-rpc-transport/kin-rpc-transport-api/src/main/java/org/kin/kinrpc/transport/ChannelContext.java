package org.kin.kinrpc.transport;

import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
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
    default void writeAndFlush(ByteBuf byteBuf){
        writeAndFlush(byteBuf, TransportOperationListener.DEFAULT);
    }

    /**
     * write out message
     * @param msg  message
     * @param listener  transport operation listener
     */
    default void writeAndFlush(ByteBuf byteBuf, @Nonnull TransportOperationListener listener){
        //目前仅有server端需要write response
        // TODO: 2023/6/9 如果后续需要扩展流协议请求, 则client端也要支持write
        throw new UnsupportedOperationException();
    }

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
