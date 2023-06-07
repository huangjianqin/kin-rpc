package org.kin.kinrpc.transport.cmd.processor;

import org.kin.kinrpc.transport.*;
import org.kin.kinrpc.transport.cmd.HeartbeatCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public class RpcRequestCommandProcessor implements CommandProcessor<RpcRequestCommand>{
    private static final Logger log = LoggerFactory.getLogger(RpcRequestCommandProcessor.class);

    @Override
    public void process(RemotingContext context, RpcRequestCommand command) {
        int timeout = command.getTimeout();
        long requestId = command.getId();
        if(timeout > 0 && System.currentTimeMillis() > timeout){
            String errorMsg = String.format("rpc request is timeout(%d), id=%d, from=%s", timeout, requestId, context.address());
            log.error(errorMsg);
            context.writeResponse(RpcResponseCommand.error(command, errorMsg));
            return;
        }

        RequestProcessorTask processorTask = new RequestProcessorTask(context, command, RequestProcessor.RPC_REQUEST_INTEREST, command);
        processorTask.run();
    }
}
