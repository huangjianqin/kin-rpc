package org.kin.kinrpc.rpc.invoker.impl;


import org.kin.kinrpc.future.RPCFuture;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.transport.rpc.ConsumerConnection;
import org.kin.kinrpc.transport.rpc.domain.RPCRequest;
import org.kin.kinrpc.transport.rpc.domain.RPCRequestIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class SimpleReferenceInvoker extends ReferenceInvoker {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    public SimpleReferenceInvoker(Class<?> interfaceClass, ConsumerConnection consumerConnection) {
        super(interfaceClass, consumerConnection);
    }

    @Override
    public void init() {
        log.info("ReferenceInvoker initing...");
        this.consumerConnection.connect();
    }

    @Override
    public void shutdown() {
        log.info("ReferenceInvoker shutdowning...");
        consumerConnection.close();
    }


    public Object invoke(String methodName, Object... params) {
        log.info("invoker method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        RPCFuture future = consumerConnection.request(request);
        try {
            return future.get();
        } catch (InterruptedException e) {
            log.info("pending result interrupted");
            e.printStackTrace();
        } catch (ExecutionException e) {
            log.info("pending result execute error");
            e.printStackTrace();
        }

        return null;
    }

    public RPCFuture invokerAsync(String methodName, Object... params) {
        log.info("invokerAsync method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        RPCFuture future = consumerConnection.request(request);
        return future;
    }
}
