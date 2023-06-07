package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RemotingCodec;
import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * remoting context
 * @author huangjianqin
 * @date 2023/5/30
 */
public class RemotingContext implements ChannelContext{
    private static final Logger log = LoggerFactory.getLogger(RemotingContext.class);

    /** 协议codec */
    private final RemotingCodec codec;
    /** {@link RequestProcessor}实例管理 */
    private final RequestProcessorManager requestProcessorManager;
    /** channel context */
    private final ChannelContext channelContext;

    public RemotingContext(RemotingCodec codec, RequestProcessorManager requestProcessorManager, ChannelContext channelContext) {
        this.codec = codec;
        this.requestProcessorManager = requestProcessorManager;
        this.channelContext = channelContext;
    }

    @Override
    public void writeAndFlush(Object msg, @Nullable TransportOperationListener listener) {
        if(!(msg instanceof RpcResponseCommand) && !(msg instanceof MessageCommand)){
            throw new TransportException("can not write message which type is" + msg.getClass().getName());
        }

        RemotingCommand remotingCommand = (RemotingCommand) msg;
        try {
            channelContext.writeAndFlush(codec.encode(remotingCommand), listener);
        } catch (Exception e) {
            String errorMsg = String.format("serialize RemotingCommand fail, id=%d, due to %s", remotingCommand.getId(), e.getMessage());
            log.error(errorMsg, e);
            if(e instanceof TransportException){
                writeError(remotingCommand, errorMsg, listener);
            }
        }
    }

    /**
     * write response with error message
     * todo 方法重命名
     * @param command   remoting command
     * @param errorMsg  error message
     */
    public void writeError(RemotingCommand command, String errorMsg){
        // TODO: 2023/6/7 response transport operation listner是否可以统一
        if(command instanceof RpcResponseCommand){
            writeAndFlush(RpcResponseCommand.error(command, errorMsg), new TransportOperationListener() {
                @Override
                public void onComplete() {
                    if(log.isDebugEnabled()){
                        log.debug("send rpc complete, id={} from {}", command.getId(),  address());
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    log.debug("send rpc fail, id={} from {}", command.getId(), address());
                }
            });
        }
        else if(command instanceof MessageCommand){
            writeAndFlush(new MessageCommand((MessageCommand) command, new Error(errorMsg)),new TransportOperationListener() {
                @Override
                public void onComplete() {
                    if(log.isDebugEnabled()){
                        log.debug("send response complete, id={} from {}", command.getId(),  address());
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    log.debug("send response fail, id={} from {}", command.getId(), address());
                }
            });
        }
    }

    /**
     * write response with error message
     * todo 方法重命名
     * @param command   remoting command
     * @param errorMsg  error message
     */
    private void writeError(RemotingCommand command, String errorMsg,  @Nullable TransportOperationListener listener){
        if(command instanceof RpcResponseCommand){
            writeAndFlush(RpcResponseCommand.error(command, errorMsg), listener);
        }
        else if(command instanceof MessageCommand){
            writeAndFlush(new MessageCommand((MessageCommand) command, new Error(errorMsg)), listener);
        }
    }

    @Override
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
        return requestProcessorManager.getByInterest(interest);
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
                    log.debug("send response complete, id={} from {}", command.getId(),  address());
                }
            }

            @Override
            public void onFailure(Throwable cause) {
                log.debug("send response fail, id={} from {}", command.getId(), address());
            }
        });
    }
}
