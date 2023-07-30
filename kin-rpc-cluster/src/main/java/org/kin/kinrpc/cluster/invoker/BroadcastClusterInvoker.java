package org.kin.kinrpc.cluster.invoker;

import org.kin.framework.utils.Extension;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.registry.directory.Directory;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/29
 */
@Extension("broadcast")
public class BroadcastClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(BroadcastClusterInvoker.class);

    /** 失败不再服务调用比例 */
    private final int failPercent;

    public BroadcastClusterInvoker(ReferenceConfig<T> referenceConfig,
                                   RegistryConfig registryConfig,
                                   Directory directory) {
        super(referenceConfig, registryConfig, directory);
        this.failPercent = referenceConfig.attachment(ReferenceConstants.BROADCAST_FAIL_PERCENT_KEY, DefaultConfig.DEFAULT_BROADCAST_FAIL_PERCENT);
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        List<ReferenceInvoker<?>> availableInvokers = directory.list();
        int totalSize = availableInvokers.size();
        //允许服务调用失败次数
        int maxFailTimes = (int) (totalSize * failPercent / 100D);

        doInvoke0(invocation, future, availableInvokers,
                0, 0, maxFailTimes);
    }

    /**
     * rpc call
     *
     * @param invocation        rpc call信息
     * @param future            completed future
     * @param availableInvokers 当前可用invoker
     * @param idx               准备要发起rpc call的invoker idx
     * @param curFailTimes      当前rpc call fail次数
     * @param maxFailTimes      最大rpc call fail次数
     */
    private void doInvoke0(Invocation invocation,
                           CompletableFuture<Object> future,
                           List<ReferenceInvoker<?>> availableInvokers,
                           int idx,
                           int curFailTimes,
                           int maxFailTimes) {
        Invocation invocationCopy = ReferenceUtils.copyInvocation(invocation);
        try {
            ReferenceInvoker<?> availableInvoker = availableInvokers.get(idx);
            invocationCopy.attach(InvocationConstants.SELECTED_INVOKER_KEY, availableInvoker);
            RpcResult rpcResult = invokeFilterChain(invocationCopy);
            rpcResult.onFinish((r, t) -> {
                if (Objects.isNull(t)) {
                    //success
                    if (idx + 1 < availableInvokers.size()) {
                        doInvoke0(invocation, future, availableInvokers, idx + 1, curFailTimes, maxFailTimes);
                    } else {
                        //broadcast end, take last rpc call result as final result
                        future.complete(r);
                    }
                } else {
                    onInvokeFail(invocationCopy, future, availableInvokers, idx, curFailTimes, maxFailTimes, t);
                }
            });
        } catch (Exception e) {
            //fail
            onInvokeFail(invocationCopy, future, availableInvokers, idx, curFailTimes, maxFailTimes, e);
        }
    }

    /**
     * rpc call fail处理
     *
     * @param invocation        rpc call信息
     * @param future            completed future
     * @param availableInvokers 当前可用invoker
     * @param idx               准备要发起rpc call的invoker idx
     * @param curFailTimes      当前rpc call fail次数
     * @param maxFailTimes      最大rpc call fail次数
     * @param t                 rpc call fail exception
     */
    private void onInvokeFail(Invocation invocation,
                              CompletableFuture<Object> future,
                              List<ReferenceInvoker<?>> availableInvokers,
                              int idx,
                              int curFailTimes,
                              int maxFailTimes,
                              Throwable t) {
        //fail
        int failTimes = curFailTimes + 1;
        if (failTimes < maxFailTimes) {
            //continue
            log.error("broadcast cluster rpc call fail {} times(maxFailTimes={}), continue to send rpc call, invocation={}", failTimes, maxFailTimes, invocation, t);
            doInvoke0(invocation, future, availableInvokers, idx + 1, failTimes, maxFailTimes);
        } else {
            //percent limit
            log.error("broadcast cluster rpc call fail {} times and has reached max fail times({}), invocation={}", failTimes, maxFailTimes, invocation, t);
        }
    }
}