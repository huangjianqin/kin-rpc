package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.InterceptorChain;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
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

    public FailFastClusterInvoker(ReferenceConfig<T> config,
                                  DefaultDirectory directory,
                                  Router router,
                                  LoadBalance loadBalance,
                                  InterceptorChain<T> interceptorChain) {
        super(config, directory, router, loadBalance, interceptorChain);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        selectAttachOrThrow(invocation, Collections.emptyList());
        RpcResult rpcResult = doInterceptorChainInvoke(invocation);
        rpcResult.onFinish((r, t) -> {
            if (log.isDebugEnabled()) {
                log.debug("rpc call result. result={}, exception={}, invocation={}", r, t, invocation);
            }
        }, future);
    }
}
