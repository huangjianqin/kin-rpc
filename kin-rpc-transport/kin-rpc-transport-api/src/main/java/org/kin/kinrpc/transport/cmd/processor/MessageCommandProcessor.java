package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.*;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public class MessageCommandProcessor implements CommandProcessor<MessageCommand>{
    private static final Logger log = LoggerFactory.getLogger(MessageCommandProcessor.class);

    @Override
    public void process(RemotingContext context, MessageCommand command) {
        int timeout = command.getTimeout();
        long requestId = command.getId();
        if(System.currentTimeMillis() > timeout){
            String errorMsg = String.format("rpc request is timeout(%d), id=%d, from=%s", timeout, requestId, context.address());
            log.error(errorMsg);
            return;
        }

        CompletableFuture<Object> future = context.removeRequestFuture(requestId);
        if (Objects.isNull(future)) {
            //received message
            RequestProcessorTask processorTask = new RequestProcessorTask(context, command, command.getInterest(), command.getData());
            processorTask.run();
        }
        else{
            //message response
            future.complete(command.getData());
        }
    }
}
