package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
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
        this.forks = config.attachment(ReferenceConstants.FORKING_FORKS_KEY, DefaultConfig.DEFAULT_FORKING_FORKS);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        if (forks < 1) {
            //尽全力发起rpc call
            List<ServiceInstance> excludes = new LinkedList<>();
            ReferenceInvoker<T> selected;
            while (Objects.nonNull(selected = select(invocation, excludes))) {
                if (Objects.nonNull(selected)) {
                    doInvoke0(invocation, future, excludes, selected);
                } else {
                    break;
                }
            }
        } else {
            //最多发起forks次rpc call
            List<ServiceInstance> excludes = new LinkedList<>();
            for (int i = 0; i < forks; i++) {
                ReferenceInvoker<T> selected = select(invocation, excludes);

                if (Objects.nonNull(selected)) {
                    doInvoke0(invocation, future, excludes, selected);
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
     * @param excludes   不包含的invoker实例
     * @param selected   本轮选中的{@link ReferenceInvoker}实例
     */
    private void doInvoke0(Invocation invocation,
                           CompletableFuture<Object> future,
                           List<ServiceInstance> excludes,
                           ReferenceInvoker<T> selected) {
        excludes.add(selected.serviceInstance());
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