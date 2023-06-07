package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RemotingCommand;
import org.kin.kinrpc.transport.cmd.RequestCommand;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.transport.cmd.RpcResponseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2023/6/2
 */
@SuppressWarnings("rawtypes")
public class RequestProcessorTask implements Runnable{
    private static final Logger log = LoggerFactory.getLogger(RequestProcessorTask.class);

    /** remoting context */
    private final RemotingContext context;
    /** request command */
    private final RequestCommand command;
    /** request obj */
    private final Serializable request;
    /** request processor */
    private final RequestProcessor requestProcessor;

    public RequestProcessorTask(RemotingContext context, RequestCommand command, String interest, Serializable request) {
        this.context = context;
        this.command = command;
        this.request = request;
        this.requestProcessor = context.getByInterest(interest);
    }

    @Override
    public void run() {
        ExecutorSelector executorSelector = requestProcessor.getExecutorSelector();
        if (Objects.nonNull(executorSelector)) {
            Executor executor = executorSelector.select(command);
            executor.execute(this::doProcess);
        }
        else{
            doProcess();
        }

    }

    @SuppressWarnings("unchecked")
    private void doProcess(){
        try {
            requestProcessor.process(new RequestContext(context, command), request);
        } catch (Exception e) {
            log.error("processor remoting request fail, id={}, from={}", command.getId(), context.address(), e);
            context.writeResponse(RpcResponseCommand.error(command, "processor remoting request fail due to " + e.getMessage()));
        }
    }
}
