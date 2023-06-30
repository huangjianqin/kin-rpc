package org.kin.kinrpc.cluster.invoker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.SPI;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.InvokerNotFoundException;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.registry.directory.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2023/6/25
 */
@SPI(alias = "cluster", singleton = false)
public abstract class ClusterInvoker<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    /** reference配置 */
    protected final ReferenceConfig<T> config;
    /** 管理订阅服务的所有invoker实例 */
    private final Directory directory;
    /** 路由策略 */
    private final Router router;
    /** 负载均衡策略 */
    private final LoadBalance loadBalance;
    /** 拦截器调用链 */
    private final InterceptorChain<T> interceptorChain;
    /** key -> 服务方法唯一id, 即handlerId, value -> 上一次服务调用成功的invoker */
    private final Cache<Integer, Invoker<T>> stickyInvokerCache = CacheBuilder.newBuilder()
            //5分钟内没有任何访问即移除
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    protected ClusterInvoker(ReferenceConfig<T> config,
                             Directory directory,
                             Router router,
                             LoadBalance loadBalance,
                             InterceptorChain<T> interceptorChain) {
        this.config = config;
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
        this.interceptorChain = interceptorChain;
    }

    @Override
    public final RpcResult invoke(Invocation invocation) {
        if (log.isDebugEnabled()) {
            log.debug("curTimes do invoke. invocation={}", invocation);
        }
        CompletableFuture<Object> future = new CompletableFuture<>();
        RpcResult rpcResult = RpcResult.success(invocation, future);
        doInvoke(invocation, future);
        return rpcResult;
    }

    /**
     * 真正的invoke逻辑实现
     *
     * @param invocation rpc call信息
     * @param future     completed future
     */
    protected abstract void doInvoke(Invocation invocation, CompletableFuture<Object> future);

    /**
     * 根据router和loadBalance策略选择一个可用的invoker实例
     *
     * @param invocation rpc call信息
     * @param excludes   不包含的invoker实例
     * @return 可用的invoker实例, 可能为null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    protected final ReferenceInvoker<T> select(Invocation invocation, Collection<ServiceInstance> excludes) {
        if (log.isDebugEnabled()) {
            log.debug("select reference invoker from cluster...");
        }

        //1. check sticky
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        if (Objects.nonNull(methodConfig) && methodConfig.isSticky()) {
            Invoker<T> invoker = stickyInvokerCache.getIfPresent(invocation.handlerId());
            if (Objects.nonNull(invoker)) {
                return (ReferenceInvoker<T>) invocation;
            }
        }

        //2. list invokers
        List<ReferenceInvoker<?>> availableInvokers = directory.list();
        //过滤掉单次请求曾经fail的service 访问地址
        availableInvokers = availableInvokers.stream().filter(invoker -> !excludes.contains(invoker.serviceInstance()))
                .collect(Collectors.toList());
        //3. route
        List<ReferenceInvoker<?>> routedInvokers = router.route(availableInvokers);
        //4. load balance
        ReferenceInvoker<?> loadBalancedInvoker = loadBalance.loadBalance(invocation, routedInvokers);

        if (log.isDebugEnabled()) {
            if (loadBalancedInvoker != null) {
                log.debug("reference invoker select finished, {}", loadBalancedInvoker.serviceInstance());
            } else {
                log.debug("can not select any available reference invoker");
            }
        }
        return (ReferenceInvoker<T>) loadBalancedInvoker;
    }

    /**
     * 根据router和loadBalance策略选择一个可用的invoker实例, 然后绑定到{@code invocation}, 如果没有找到任何可用invoker, 则抛{@link InvokerNotFoundException}
     *
     * @param invocation rpc call信息
     * @param excludes   不包含的invoker实例
     * @return 可用的invoker实例, 可能为null
     */
    protected final ReferenceInvoker<T> selectAttachOrThrow(Invocation invocation, Collection<ServiceInstance> excludes) throws InvokerNotFoundException {
        ReferenceInvoker<T> selected = select(invocation, excludes);

        if (Objects.nonNull(selected)) {
            invocation.attach(ReferenceConstants.SELECTED_INVOKER_KEY, selected);
        } else {
            throw new InvokerNotFoundException(invocation.handler());
        }

        return selected;
    }

    /**
     * 执行拦截器调用链
     *
     * @param invocation rpc call信息
     * @return rpc call result
     */
    protected final RpcResult doInterceptorChainInvoke(Invocation invocation) {
        if (log.isDebugEnabled()) {
            log.debug("cluster invoker do interceptor chain invoke. invocation={}", invocation);
        }

        try {
            RpcResult rpcResult = interceptorChain.invoke(invocation);
            rpcResult.onFinish((r, t) -> {
                MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
                if (Objects.isNull(methodConfig) || !methodConfig.isSticky()) {
                    return;
                }

                //维护服务方法调用sticky
                if (Objects.isNull(t)) {
                    //success
                    Invoker<T> invoker = invocation.attachment(ReferenceConstants.SELECTED_INVOKER_KEY);
                    if (Objects.nonNull(invoker)) {
                        stickyInvokerCache.put(invocation.handlerId(), invoker);
                    }
                } else {
                    stickyInvokerCache.invalidate(invocation.handlerId());
                }
            });
            return rpcResult;
        } catch (Exception e) {
            return RpcResult.fail(invocation, e);
        }
    }

    /**
     * 重置或清掉一次rpc call中的临时信息, 用于下次重试时, 重新赋值, 避免干扰
     *
     * @param invocation rpc call信息
     */
    protected final void onResetInvocation(Invocation invocation) {
        invocation.clear();
    }

    /**
     * 释放占用资源
     */
    public final void destroy() {
        // TODO: 2023/6/26 loadbalance router释放unsubscribe服务订阅, 或者基于cache clearafterread
        //是否转移到registry unsubscribe更好
        directory.destroy();
    }
}
