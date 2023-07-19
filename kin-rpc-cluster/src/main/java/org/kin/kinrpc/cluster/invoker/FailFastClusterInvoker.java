package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class FailFastClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailFastClusterInvoker.class);

    public FailFastClusterInvoker(ReferenceConfig<T> config) {
        super(config);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        selectAttachOrThrow(invocation, Collections.emptyList());
        RpcResult rpcResult = invokeFilterChain(invocation);
        rpcResult.onFinish(future);
    }
}
