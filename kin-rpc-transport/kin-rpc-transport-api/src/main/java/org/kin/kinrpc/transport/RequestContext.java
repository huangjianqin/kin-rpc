package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * request process context
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RequestContext{
    private static final Logger log = LoggerFactory.getLogger(RequestContext.class);

    /** remoting context */
    private final RemotingContext remotingContext;
    /** request cmd */
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
        remotingContext.writeResponseIfError(command, t.getClass().getName() + ": " + t.getMessage());
    }

    /**
     * write message response
     * @param message   message response result
     */
    public void writeMessageResponse(Serializable message) {
        if (Objects.isNull(message)) {
            throw new IllegalArgumentException("does not support response empty message");
        }
        remotingContext.writeResponse(new MessageCommand((MessageCommand) command, message));
    }
}
