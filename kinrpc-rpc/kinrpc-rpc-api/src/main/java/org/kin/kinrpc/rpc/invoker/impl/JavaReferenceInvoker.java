package org.kin.kinrpc.rpc.invoker.impl;


import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class JavaReferenceInvoker extends AbstractReferenceInvoker {
    public JavaReferenceInvoker(String serviceName, RPCReference rpcReference) {
        super(serviceName, rpcReference);
    }

    @Override
    public void init() {
        this.rpcReference.start();
        log.info("ReferenceInvoker inited");
    }

    @Override
    public void shutdown() {
        rpcReference.shutdown();
        log.info("ReferenceInvoker shutdown");
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Exception {
        try {
            Future<RPCResponse> future = invoke0(methodName, params);
            if (!isVoid) {
                RPCResponse rpcResponse = future.get();
                if (rpcResponse != null) {
                    switch (rpcResponse.getState()) {
                        case SUCCESS:
                            return rpcResponse.getResult();
                        case RETRY:
                            throw new RuntimeException("need to retry to invoke service");
                        case ERROR:
                            throw new RuntimeException(rpcResponse.getInfo());
                        default:
                            throw new RuntimeException("unknown rpc response state code");
                    }
                }
            }
            return null;
        } catch (InterruptedException e) {
            log.error("pending result interrupted >>> {}", e.getMessage());
            throw e;
        } catch (ExecutionException e) {
            log.error("pending result execute error >>> {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public Future<RPCResponse> invokeAsync(String methodName, Object... params) throws Exception {
        return invoke0(methodName, params);
    }

    private Future<RPCResponse> invoke0(String methodName, Object... params){
        log.debug("invoke method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }
}
