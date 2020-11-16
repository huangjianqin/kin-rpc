package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.rpc.RpcRequest;
import org.kin.kinrpc.rpc.RpcResponse;
import org.kin.kinrpc.rpc.RpcThreadPool;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020/11/4
 */
public class KinRpcReferenceInvoker<T> extends ReferenceInvoker<T> {
    protected static final Logger log = LoggerFactory.getLogger(KinRpcReferenceInvoker.class);
    private final KinRpcReference rpcReference;

    public KinRpcReferenceInvoker(Url url) {
        super(url);
        this.rpcReference = new KinRpcReference(url);
        this.rpcReference.connect();
    }

    @Override
    public boolean isAvailable() {
        return rpcReference.isActive();
    }

    @Override
    public void destroy() {
        rpcReference.shutdown();
        log.info("referenceInvoker destroy");
    }

    protected RpcRequest createRequest(long requestId, String methodName, Object... params) {
        return new RpcRequest(requestId, super.getServiceName(), methodName, params);
    }

    @Override
    public final Object invoke(String methodName, Object... params) throws Exception {
        try {
            Future<RpcResponse> future = invoke0(methodName, params);
            RpcResponse rpcResponse = future.get();
            if (rpcResponse != null) {
                switch (rpcResponse.getState()) {
                    case SUCCESS:
                        return rpcResponse.getResult();
                    case RETRY:
                        throw new RpcRetryException(rpcResponse.getInfo(), getServiceName(), methodName, params);
                    case ERROR:
                        throw new RpcCallErrorException(rpcResponse.getInfo());
                    default:
                        throw new UnknownRpcResponseStateCodeException(rpcResponse.getState().getCode());
                }
            } else {
                throw new RpcCallErrorException("no rpc response");
            }
        } catch (InterruptedException e) {
            log.error("pending result interrupted >>> {}", e.getMessage());
            throw e;
        } catch (ExecutionException e) {
            log.error("pending result execute error >>> {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public final Future<Object> invokeAsync(String methodName, Object... params) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                long retryTimeout = Long.parseLong(url.getParam(Constants.RETRY_TIMEOUT_KEY));
                return invoke0(methodName, params).get(retryTimeout, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, RpcThreadPool.EXECUTORS);
    }

    protected Future<RpcResponse> invoke0(String methodName, Object... params) {
        log.debug("invoke method '" + methodName + "'");
        RpcRequest request = createRequest(KinRpcRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }
}
