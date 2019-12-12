package org.kin.kinrpc.rpc.invoker.impl;


import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.exception.RPCCallErrorException;
import org.kin.kinrpc.rpc.exception.RPCRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRPCResponseStateCodeException;
import org.kin.kinrpc.rpc.invoker.AbstractInvoker;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCRequestIdGenerator;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Created by 健勤 on 2017/2/15.
 */
public class ReferenceInvoker extends AbstractInvoker implements AsyncInvoker {
    protected static final Logger log = LoggerFactory.getLogger(ReferenceInvoker.class);
    protected RPCReference rpcReference;

    public ReferenceInvoker(String serviceName, RPCReference rpcReference) {
        super(serviceName);
        this.rpcReference = rpcReference;
    }

    public void init() {
        this.rpcReference.start();
        log.info("referenceInvoker inited");
    }

    public void shutdown() {
        rpcReference.shutdown();
        log.info("referenceInvoker shutdown");
    }

    protected RPCRequest createRequest(String requestId, String methodName, Object... params) {
        RPCRequest request = new RPCRequest(requestId, super.getServiceName(), methodName, params);
        return request;
    }

    public HostAndPort getAddress() {
        return rpcReference.getAddress();
    }

    public boolean isActive() {
        return rpcReference.isActive();
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

    private Future<RPCResponse> invoke0(String methodName, Object... params) {
        log.debug("invoke method '" + methodName + "'");
        RPCRequest request = createRequest(RPCRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReferenceInvoker that = (ReferenceInvoker) o;
        return Objects.equals(rpcReference, that.rpcReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rpcReference);
    }
}