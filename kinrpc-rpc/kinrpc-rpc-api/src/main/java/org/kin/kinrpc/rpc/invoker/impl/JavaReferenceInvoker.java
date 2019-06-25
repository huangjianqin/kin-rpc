package org.kin.kinrpc.rpc.invoker.impl;


import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class JavaReferenceInvoker extends AbstractReferenceInvoker {
    public JavaReferenceInvoker(String serviceName, RPCReference rpcReference) {
        super(serviceName, rpcReference);
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
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Exception {
        try {
            Future future = invoke0(methodName, params);
            if (!isVoid) {
                return future.get();
            }
            return null;
        } catch (InterruptedException e) {
            log.info("pending result interrupted >>> {}", e.getMessage());
            throw e;
        } catch (ExecutionException e) {
            log.info("pending result execute error >>> {}", e.getMessage());
            throw e;
        } catch (TimeoutException e) {
            log.info("invoke time out >>> {}", e.getMessage());
            return null;
        }
    }

    @Override
    public Future invokeAsync(String methodName, Object... params) throws Exception {
        return invoke0(methodName, params);
    }

    private Future invoke0(String methodName, Object... params) throws Exception {
        log.info("invoke method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }
}
