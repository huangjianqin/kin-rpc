package org.kin.kinrpc.rpc.invoker.impl;


import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class SimpleReferenceInvoker extends ReferenceInvoker {
    private static final Logger log = LoggerFactory.getLogger("invoker");

    public SimpleReferenceInvoker(Class<?> interfaceClass, RPCReference rpcReference) {
        super(interfaceClass, rpcReference);
    }

    @Override
    public void init() {
        log.info("ReferenceInvoker initing...");
        this.rpcReference.start();
    }

    @Override
    public void shutdown() {
        log.info("ReferenceInvoker shutdowning...");
        rpcReference.shutdown();
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        log.info("invoker method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        Future<RPCResponse> future = rpcReference.request(request);
        try {
            if (!isVoid) {
                return future.get(200, TimeUnit.MILLISECONDS);
            }
            return null;
        } catch (InterruptedException e) {
            log.info("pending result interrupted");
            ExceptionUtils.log(e);
            throw e;
        } catch (ExecutionException e) {
            log.info("pending result execute error");
            ExceptionUtils.log(e);
            throw e;
        }
    }

    @Override
    public Future invokerAsync(String methodName, Object... params) throws Throwable {
        log.info("invokerAsync method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }
}
