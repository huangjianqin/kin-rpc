package org.kin.kinrpc.cluster.invoker;

import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.protocol.ServerErrorException;
import org.kin.kinrpc.registry.directory.Directory;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/6/26
 */
public class FailoverClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailoverClusterInvoker.class);

    public FailoverClusterInvoker(ReferenceConfig<T> config,
                                  Directory directory,
                                  Router router,
                                  LoadBalance loadBalance,
                                  FilterChain<T> filterChain) {
        super(config, directory, router, loadBalance, filterChain);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            throw new IllegalStateException("can not find method config. invocation=" + invocation);
        }

        int maxTimes = Math.max(methodConfig.getRetries(), 0);
        doInvoke(invocation, future, 1, maxTimes + 1, new HashSet<>());
    }

    /**
     * 真正的invoke逻辑实现
     *
     * @param invocation rpc call信息
     * @param future     completed future
     * @param curTimes   当前已执行次数, 从1开始
     * @param maxTimes   最大执行次数
     * @param excludes   调用失败的service instance
     */
    private void doInvoke(Invocation invocation,
                          CompletableFuture<Object> future,
                          int curTimes,
                          int maxTimes,
                          Set<ServiceInstance> excludes) {
        if (curTimes > maxTimes) {
            future.completeExceptionally(new RpcException(String.format("rpc call fail after %d times, invocation=%s", maxTimes, invocation)));
            return;
        }
        try {
            selectAttachOrThrow(invocation, excludes);
            invokeFilterChain(invocation).onFinish((r, t) -> {
                if (Objects.isNull(t)) {
                    //success
                    future.complete(r);
                } else {
                    t = RpcUtils.normalizeException(t);

                    //fail
                    log.warn("rpc call fail {} times, ready to retry rpc call, invocation={}, exception={}", curTimes, invocation, t);
                    if (!(t instanceof ServerErrorException) && t instanceof RpcException) {
                        //rpc异常(非服务方法执行异常), 才发起rpc异常重试
                        ReferenceInvoker<T> invoker = invocation.attachment(ReferenceConstants.SELECTED_INVOKER_KEY);
                        if (Objects.nonNull(invoker)) {
                            excludes.add(invoker.serviceInstance());
                        }
                        onResetInvocation(invocation);
                        doInvoke(invocation, future, curTimes + 1, maxTimes, excludes);
                    } else {
                        //其他异常
                        future.completeExceptionally(t);
                    }
                }
            });
        } catch (Exception e) {
            //其他异常
            future.completeExceptionally(e);
        }
    }
}
