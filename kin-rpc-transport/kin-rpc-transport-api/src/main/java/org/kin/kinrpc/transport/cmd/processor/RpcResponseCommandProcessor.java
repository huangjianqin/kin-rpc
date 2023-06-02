package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.RemotingContext;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RpcResponseCommandProcessor implements CommandProcessor<RpcResponseCommand>{
    private static final Logger log = LoggerFactory.getLogger(RpcResponseCommandProcessor.class);

    @Override
    public void process(RemotingContext context, RpcResponseCommand command) {
        long requestId = command.getId();
        CompletableFuture<Object> future = context.removeRequestFuture(requestId);
        if (Objects.nonNull(future)) {
            if(log.isDebugEnabled()){
                log.debug("receive rpc response, id={}", requestId);
            }

            future.complete(command);
        }
        else{
            log.error("can not find rpc request future, id={}", requestId);
        }
    }
}
