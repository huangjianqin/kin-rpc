package org.kin.kinrpc.transport;

import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.transport.cmd.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.concurrent.CompletableFuture;

/**
 * request process context
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RequestContext{
    private static final Logger log = LoggerFactory.getLogger(RequestContext.class);

    /** remoting context */
    private final RemotingContext remotingContext;
    /** cmd */
    private final RequestCommand command;

    public RequestContext(RemotingContext remotingContext, RequestCommand command) {
        this.remotingContext = remotingContext;
        this.command = command;
    }

    /**
     * write response
     * @param result   request process result
     */
    public void writeResponse(Object result){
        remotingContext.writeResponse(RpcResponseCommand.success(command, result));
    }

    /**
     * write response with error message
     * @param t   request process exception
     */
    public void writeResponseIfError(Throwable t){
        remotingContext.writeResponse(RpcResponseCommand.error(command, t.getMessage()));
    }
}
