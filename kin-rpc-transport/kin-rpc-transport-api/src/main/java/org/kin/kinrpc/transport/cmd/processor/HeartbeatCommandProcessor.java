package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.RemotingContext;
import org.kin.kinrpc.transport.TransportOperationListener;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * client主动发送心跳给server
 * server response requestId相同的心跳包给client
 * @author huangjianqin
 * @date 2023/6/1
 */
public class HeartbeatCommandProcessor implements CommandProcessor<HeartbeatCommand>{
    private static final Logger log = LoggerFactory.getLogger(HeartbeatCommandProcessor.class);

    @Override
    public void process(RemotingContext context, HeartbeatCommand command) {
        long requestId = command.getId();
        CompletableFuture<?> future = context.removeRequestFuture(requestId);
        if (Objects.isNull(future)) {
            //server receive heart beat from client
            if(log.isDebugEnabled()){
                log.debug("receive heart beat, id={} from {}", requestId, context.address());
            }

            HeartbeatCommand ack = new HeartbeatCommand(command.getVersion(), requestId);
            context.writeResponse(ack);
        }
        else{
            //client receive heart beat ack
            if(log.isDebugEnabled()){
                log.debug("receive heart beat ack, id={} from {}", requestId, context.address());
            }

            future.complete(null);
        }
    }
}
