package org.kin.kinrpc.rpc.invoker.impl;


import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceInvokerImpl extends AbstractReferenceInvoker {
    private RateLimiter rateLimiter = RateLimiter.create(Constants.REFERENCE_REQUEST_THRESHOLD);

    public ReferenceInvokerImpl(String serviceName, RPCReference rpcReference) {
        super(serviceName, rpcReference);
    }

    @Override
    public void init() {
        this.rpcReference.start();
        log.info("referenceInvoker inited");
    }

    @Override
    public void shutdown() {
        rpcReference.shutdown();
        log.info("referenceInvoker shutdown");
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
                            throw new RPCRetryException(rpcResponse.getInfo(), serviceName, methodName, params);
                        case ERROR:
                            throw new RPCCallErrorException(rpcResponse.getInfo());
                        default:
                            throw new UnknownRPCResponseStateCodeException(rpcResponse.getState().getCode());
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

    @Override
    public void setRate(double rate) {
        rateLimiter.setRate(rate);
    }

    @Override
    public double getRate() {
        return rateLimiter.getRate();
    }
}
