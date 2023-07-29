package org.kin.kinrpc.cluster.invoker;

import io.netty.util.HashedWheelTimer;
import io.netty.util.Timeout;
import io.netty.util.Timer;
import io.netty.util.TimerTask;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.utils.Extension;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.ReferenceInvoker;
import org.kin.kinrpc.RpcResult;
import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.DefaultConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.constants.KinRpcSystemProperties;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 失败自动恢复(定时重试)
 *
 * @author huangjianqin
 * @date 2023/7/29
 */
@Extension("failback")
public class FailBackClusterInvoker<T> extends ClusterInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(FailBackClusterInvoker.class);

    /** 重试间隔(s) */
    private static final int RETRY_PERIOD = SysUtils.getIntSysProperty(KinRpcSystemProperties.FAILBACK_RETRY_PERIOD, 5);

    /** 重试次数 */
    private final int maxRetries;
    /** 最大重试任务数量 */
    private final int maxRetryTask;
    /** 重试timer */
    private volatile Timer timer;

    public FailBackClusterInvoker(ReferenceConfig<T> config) {
        super(config);
        this.maxRetries = config.attachment(ReferenceConstants.FAILBACK_RETRIES_KEY, DefaultConfig.DEFAULT_FAILBACK_RETRIES);
        this.maxRetryTask = config.attachment(ReferenceConstants.FAILBACK_RETRY_TASK_KEY, DefaultConfig.DEFAULT_FAILBACK_RETRY_TASK);
    }

    /**
     * trigger when rpc call fail
     *
     * @param invocation rpc call信息
     * @param future     completed future
     */
    private void onFailure(Invocation invocation,
                           CompletableFuture<Object> future) {
        if (Objects.isNull(timer)) {
            synchronized (this) {
                if (Objects.isNull(timer)) {
                    timer = new HashedWheelTimer(new SimpleThreadFactory("kinrpc-failback-timer", true),
                            1, TimeUnit.SECONDS, 32,
                            true, maxRetryTask);
                }
            }
        }

        RetryTask retryTask = new RetryTask(invocation, future, invocation.attachment(InvocationConstants.SELECTED_INVOKER_KEY));
        try {
            timer.newTimeout(retryTask, RETRY_PERIOD, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("failback cluster retry fail, due to retry task number limit, invocation={}", invocation, e);
        }
    }

    @Override
    protected void doInvoke(Invocation invocation, CompletableFuture<Object> future) {
        selectAttachOrThrow(invocation, Collections.emptyList());
        RpcResult rpcResult = invokeFilterChain(invocation);
        rpcResult.onFinish((r, t) -> {
            if (Objects.isNull(t)) {
                //success
                future.complete(r);
            } else {
                //fail
                log.error("failback cluster rpc call fail, waiting to retry", RpcUtils.normalizeException(t));
                onFailure(invocation, future);
            }
        });
    }

    //---------------------------------------------------------------------------------------------------------

    /**
     * 重试任务
     */
    private class RetryTask implements TimerTask {
        /** rpc call info */
        private final Invocation invocation;
        /** completed future */
        private final CompletableFuture<Object> future;
        /** rpc call fail invoker */
        private final List<ServiceInstance> failInstances = new LinkedList<>();

        /** 重试次数 */
        private int retryTimes = 0;

        public RetryTask(Invocation invocation,
                         CompletableFuture<Object> future,
                         ReferenceInvoker<?> failInvoker) {
            this.invocation = invocation;
            this.future = future;
            if (Objects.nonNull(failInvoker)) {
                this.failInstances.add(failInvoker.serviceInstance());
            }
        }

        @Override
        public void run(Timeout timeout) {
            log.info("failback cluster ready to retry to rpc call (current {} times, total {} times), invocation={}", retryTimes, maxRetries, invocation);
            selectAttachOrThrow(invocation, failInstances);
            RpcResult rpcResult = invokeFilterChain(invocation);
            rpcResult.onFinish((r, t) -> {
                if (Objects.isNull(t)) {
                    //success
                    future.complete(r);
                } else {
                    //fail
                    if ((++retryTimes) >= maxRetries) {
                        log.error("failback cluster total retry to rpc call fail {} times(total {} times))", retryTimes, maxRetries, t);
                    } else {
                        log.error("failback cluster retry to rpc call fail, waiting to retry", t);
                        ReferenceInvoker<?> referenceInvoker = invocation.attachment(InvocationConstants.SELECTED_INVOKER_KEY);
                        if (Objects.nonNull(referenceInvoker)) {
                            failInstances.add(referenceInvoker.serviceInstance());
                        }
                        timer.newTimeout(this, RETRY_PERIOD, TimeUnit.SECONDS);
                    }
                }
            });
        }
    }

}