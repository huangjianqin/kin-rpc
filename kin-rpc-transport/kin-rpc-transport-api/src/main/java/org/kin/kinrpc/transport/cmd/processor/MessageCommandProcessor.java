package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.RemotingContext;
import org.kin.kinrpc.transport.RemotingException;
import org.kin.kinrpc.transport.RequestProcessTask;
import org.kin.kinrpc.transport.cmd.MessageCommand;
import org.kin.kinrpc.transport.message.Error;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
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
        long timeout = command.getTimeout();
        long requestId = command.getId();
        if(timeout > 0 && System.currentTimeMillis() > timeout){
            String errorMsg = String.format("message request is timeout(%d), id=%d, from=%s", timeout, requestId, context.address());
            log.error(errorMsg);
            return;
        }

        CompletableFuture<Object> future = context.removeRequestFuture(requestId);
        Serializable data = command.getData();
        if (Objects.isNull(future)) {
            //received message
            if(data instanceof Error){
                log.warn("receive Error message, just ignore");
            }
            else{
                RequestProcessTask processorTask = new RequestProcessTask(context, command, command.getInterest(), data);
                processorTask.run();
            }
        }
        else{
            //message response
            if(data instanceof Error){
                //error response
                future.completeExceptionally(new RemotingException(((Error) data).getMessage()));
            }
            else{
                future.complete(data);
            }
        }
    }
}
