package org.kin.kinrpc.rpc.invoker.impl;


import com.google.common.net.HostAndPort;
import org.kin.kinrpc.rpc.RpcReference;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;
import org.kin.kinrpc.rpc.invoker.AbstractInvoker;
import org.kin.kinrpc.rpc.invoker.AsyncInvoker;
import org.kin.kinrpc.rpc.transport.RpcRequest;
import org.kin.kinrpc.rpc.transport.RpcResponse;
import org.kin.kinrpc.transport.domain.RpcRequestIdGenerator;
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
    protected RpcReference rpcReference;

    public ReferenceInvoker(String serviceName, RpcReference rpcReference) {
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

    protected RpcRequest createRequest(long requestId, String methodName, Object... params) {
        return new RpcRequest(requestId, super.getServiceName(), methodName, params);
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
            Future<RpcResponse> future = invoke0(methodName, params);
            if (!isVoid) {
                RpcResponse rpcResponse = future.get();
                if (rpcResponse != null) {
                    switch (rpcResponse.getState()) {
                        case SUCCESS:
                            return rpcResponse.getResult();
                        case RETRY:
                            throw new RpcRetryException(rpcResponse.getInfo(), serviceName, methodName, params);
                        case ERROR:
                            throw new RpcCallErrorException(rpcResponse.getInfo());
                        default:
                            throw new UnknownRpcResponseStateCodeException(rpcResponse.getState().getCode());
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
    public Future<RpcResponse> invokeAsync(String methodName, Object... params) {
        return invoke0(methodName, params);
    }

    private Future<RpcResponse> invoke0(String methodName, Object... params) {
        log.debug("invoke method '" + methodName + "'");
        RpcRequest request = createRequest(RpcRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        ReferenceInvoker that = (ReferenceInvoker) o;
        return Objects.equals(rpcReference, that.rpcReference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), rpcReference);
    }
}