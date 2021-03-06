package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

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
        return new RpcRequest(requestId, url, methodName, params);
    }

    @Override
    public final Object invoke(String methodName, Object[] params) throws Throwable {
        try {
            CompletableFuture<Object> future = invoke0(methodName, params);
            return future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Override
    public final CompletableFuture<Object> invokeAsync(String methodName, Object... params) {
        return invoke0(methodName, params);
    }

    protected CompletableFuture<Object> invoke0(String methodName, Object... params) {
        log.debug("invoke method '" + methodName + "'");
        RpcRequest request = createRequest(KinRpcRequestIdGenerator.next(), methodName, params);
        return rpcReference.request(request);
    }
}
