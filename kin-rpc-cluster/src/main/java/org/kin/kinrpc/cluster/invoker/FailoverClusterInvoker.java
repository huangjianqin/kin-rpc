package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.InterceptorChain;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.RpcException;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.protocol.RpcBizException;
import org.kin.kinrpc.registry.directory.DefaultDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class FailoverClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(ReferenceConfig<T> config,
                                  DefaultDirectory directory,
                                  Router router,
                                  LoadBalance loadBalance,
                                  InterceptorChain<T> interceptorChain) {
        super(config, directory, router, loadBalance, interceptorChain);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            throw new IllegalStateException("can not find method config. invocation=" + invocation);
        }

        doInvoke(invocation, future, 1, methodConfig.getRetries());
    }

    /**
     * 真正的invoke逻辑实现
     *
     * @param invocation rpc call信息
     * @param future     completed future
     * @param curTimes   当前已执行次数, 从1开始
     * @param maxRetries 最大重试次数
     */
    private void doInvoke(Invocation invocation,
                          CompletableFuture<Object> future,
                          int curTimes,
                          int maxRetries) {
        if (curTimes > maxRetries) {
            future.completeExceptionally(new RpcException(String.format("rpc call fail after %d times, invocation=%s", maxRetries, invocation)));
            return;
        }
        selectAttachOrThrow(invocation, Collections.emptyList());
        doInterceptorChainInvoke(invocation).onFinish((r, t) -> {
            if (log.isDebugEnabled()) {
                log.debug("rpc call result after {} times retries. result={}, exception={}, invocation={}", curTimes, r, t, invocation);
            }

            if (Objects.isNull(t)) {
                //success
                future.complete(r);
            } else {
                //fail
                log.warn("rpc call fail {} times, invocation={}, exception={}", curTimes, invocation, t);
                if (!(t instanceof RpcBizException)) {
                    //非服务方法调用异常, 重试
                    onResetInvocation(invocation);
                    doInvoke(invocation, future, curTimes + 1, maxRetries);
                } else {
                    future.completeExceptionally(t);
                }
            }
        });
    }
}
