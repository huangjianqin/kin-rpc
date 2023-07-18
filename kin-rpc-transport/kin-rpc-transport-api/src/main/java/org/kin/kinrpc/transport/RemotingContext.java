package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.*;
import org.kin.kinrpc.transport.message.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * remoting context
 * @author huangjianqin
 * @date 2023/5/30
 */
public class RemotingContext{
    private static final Logger log = LoggerFactory.getLogger(RemotingContext.class);

    /** 协议codec */
    private final RemotingCodec codec;
    /** {@link RequestProcessor}实例管理 */
    private final RequestProcessorRegistry requestProcessorRegistry;
    /** channel context */
    private final ChannelContext channelContext;

    public RemotingContext(RemotingCodec codec, RequestProcessorRegistry requestProcessorRegistry, ChannelContext channelContext) {
        this.codec = codec;
        this.requestProcessorRegistry = requestProcessorRegistry;
        this.channelContext = channelContext;
    }

    /**
     * write response command
     * @param command  response command
     * @param listener  transport operation listener
     */
    private void writeAndFlush(RemotingCommand command, @Nonnull TransportOperationListener listener) {
        if (!(command instanceof RpcResponseCommand) &&
                !(command instanceof MessageCommand) &&
                !(command instanceof HeartbeatCommand)) {
            throw new TransportException("can not write message which type is" + command.getClass().getName());
        }

        try {
            channelContext.writeAndFlush(codec.encode(command), listener);
        } catch (Exception e) {
            String errorMsg = String.format("write response command fail, id=%d, due to %s", command.getId(), e.getClass().getName() + ": " + e.getMessage());
            log.error("write response command fail, id={}", command.getId(), e);
            if (!(e instanceof CodecException)) {
                writeResponseIfError(command, errorMsg, listener);
            }
        }
    }

    /**
     * write response with error message
     * @param command   remoting command
     * @param errorMsg  error message
     */
    public void writeResponseIfError(RemotingCommand command, String errorMsg){
        RemotingCommand respondCommand = null;
        if (command instanceof RpcRequestCommand) {
            respondCommand = RpcResponseCommand.error(command, errorMsg);
        } else if (command instanceof MessageCommand) {
            respondCommand = new MessageCommand((MessageCommand) command, new Error(errorMsg));
        }

        if (Objects.nonNull(respondCommand)) {
            writeResponse(respondCommand);
        }
    }

    /**
     * write response with error message
     * @param command   remoting command
     * @param errorMsg  error message
     * @param listener transport operation listener
     */
    private void writeResponseIfError(RemotingCommand command, String errorMsg, @Nonnull TransportOperationListener listener){
        RemotingCommand respondCommand = null;
        if (command instanceof RpcRequestCommand) {
            respondCommand = RpcResponseCommand.error(command, errorMsg);
        } else if (command instanceof MessageCommand) {
            respondCommand = new MessageCommand((MessageCommand) command, new Error(errorMsg));
        }

        if (Objects.nonNull(respondCommand)) {
            writeAndFlush(respondCommand, listener);
        }
    }

    /**
     * 返回client address
     * @return  client address
     */
    public SocketAddress address() {
        return channelContext.address();
    }

    /**
     * 移除并返回request future
     * server侧永远返回null
     * @param requestId request id
     * @return  request future
     */
    @Nullable
    public CompletableFuture<Object> removeRequestFuture(long requestId){
        return channelContext.removeRequestFuture(requestId);
    }

    /**
     * 根据interest返回对应的{@link RequestProcessor}实例
     * @param interest request processor interest
     * @return  {@link RequestProcessor}实例
     */
    public RequestProcessor<?> getByInterest(String interest){
        return requestProcessorRegistry.getByInterest(interest);
    }

    /**
     * write response
     * @param command   response command
     */
    public void writeResponse(RemotingCommand command){
        writeAndFlush(command, new TransportOperationListener() {
            @Override
            public void onComplete() {
                if(log.isDebugEnabled()){
                    log.debug("send response complete, id={} to {}", command.getId(),  address());
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                if(log.isDebugEnabled()){
                    log.debug("send response fail, id={} to {}", command.getId(), address());
                }
            }
        });
    }
}
