package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 并行发起服务调用
 *
 * @author huangjianqin
 * @date 2023/7/29
 */
@Extension("forking")
public class ForkingClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ForkingClusterInvoker.class);

    /** 并行数 */
    private final int forks;

    public ForkingClusterInvoker(ReferenceConfig<T> config) {
        super(config);
        this.forks = config.intAttachment(ReferenceConstants.BROADCAST_FAIL_PERCENT_KEY, DefaultConfig.DEFAULT_FORKING_FORKS);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        List<ServiceInstance> excludes = new CopyOnWriteArrayList<>();
        if (forks < 1) {
            //尽全力发起rpc call
            for (ReferenceInvoker<?> invoker : directory.list()) {
                Invocation invocationCopy = ReferenceUtils.copyInvocation(invocation);
                ReferenceContext.SCHEDULER.execute(() -> doInvoke0(invocationCopy, future, (ReferenceInvoker<T>) invoker));
            }
        } else {
            //最多发起forks次rpc call
            for (int i = 0; i < forks; i++) {
                Invocation invocationCopy = ReferenceUtils.copyInvocation(invocation);
                ReferenceInvoker<T> selected = select(invocationCopy, excludes);

                if (Objects.nonNull(selected)) {
                    excludes.add(selected.serviceInstance());

                    ReferenceContext.SCHEDULER.execute(() -> doInvoke0(invocationCopy, future, selected));
                } else {
                    break;
                }
            }
        }
    }

    /**
     * rpc call
     *
     * @param invocation rpc call信息
     * @param future     completed future
     * @param selected   本轮选中的{@link ReferenceInvoker}实例
     */
    private void doInvoke0(Invocation invocation,
                           CompletableFuture<Object> future,
                           ReferenceInvoker<T> selected) {
        invocation.attach(InvocationConstants.SELECTED_INVOKER_KEY, selected);

        RpcResult rpcResult = invokeFilterChain(invocation);
        rpcResult.onFinish((r, t) -> onRpcCallResponse(invocation, future, r, t));
    }

    /**
     * rpc call response处理
     *
     * @param invocation rpc call信息
     * @param future     completed future
     * @param result     rpc call result
     * @param t          rpc call exception
     */
    private void onRpcCallResponse(Invocation invocation,
                                   CompletableFuture<Object> future,
                                   Object result,
                                   Throwable t) {
        if (future.isDone()) {
            //ignore
            return;
        }

        if (Objects.isNull(t)) {
            //success
            future.complete(result);
        } else {
            //fail
            log.error("forking cluster rpc call fail, invocation={}", invocation, t);
        }
    }
}