package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * 快速失败
 *
 * @author huangjianqin
 * @date 2023/6/26
 */
@Extension("failfast")
public class FailFastClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailFastClusterInvoker.class);

    public FailFastClusterInvoker(ReferenceConfig<T> referenceConfig,
                                  RegistryConfig registryConfig,
                                  Directory directory) {
        super(referenceConfig, registryConfig, directory);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        selectAttachOrThrow(invocation, Collections.emptyList());
        RpcResult rpcResult = invokeFilterChain(invocation);
        rpcResult.onFinish(future);
    }
}
