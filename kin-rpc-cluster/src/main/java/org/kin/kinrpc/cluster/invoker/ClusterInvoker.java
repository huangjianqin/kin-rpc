package org.kin.kinrpc.cluster.invoker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.CollectionUtils;
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
    /** filter chain */
    private final FilterChain<T> filterChain;
    /** key -> 服务方法唯一id, 即handlerId, value -> 上一次服务调用成功的invoker */
    private final Cache<Integer, ReferenceInvoker<T>> stickyInvokerCache = CacheBuilder.newBuilder()
            //5分钟内没有任何访问即移除
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    protected ClusterInvoker(ReferenceConfig<T> config,
                             Directory directory,
                             Router router,
                             LoadBalance loadBalance,
                             FilterChain<T> filterChain) {
        this.config = config;
        this.directory = directory;
        this.router = router;
        this.loadBalance = loadBalance;
        this.filterChain = filterChain;
    }

    @Override
    public final RpcResult invoke(Invocation invocation) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        RpcResult rpcResult = RpcResult.success(invocation, future);
        doInvoke(invocation, future);
        return rpcResult;
    }

    /**
     * 自定义invoke实现
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
            ReferenceInvoker<T> invoker = stickyInvokerCache.getIfPresent(invocation.handlerId());
            if (Objects.nonNull(invoker)) {
                if (log.isDebugEnabled()) {
                    log.debug("reference invoker select finished, {}", invoker.serviceInstance());
                }
                return invoker;
            }
        }

        //2. list invokers
        List<ReferenceInvoker<?>> availableInvokers = directory.list();
        //过滤掉单次请求曾经fail的service 访问地址
        availableInvokers = availableInvokers.stream().filter(invoker -> !excludes.contains(invoker.serviceInstance()))
                .collect(Collectors.toList());
        //3. route
        List<ReferenceInvoker<?>> routedInvokers = router.route(availableInvokers);
        if (CollectionUtils.isEmpty(routedInvokers)) {
            return null;
        }

        //4. load balance
        ReferenceInvoker<?> loadBalancedInvoker = loadBalance.loadBalance(invocation, routedInvokers);

        //attach
        invocation.attach(ReferenceConstants.LOADBALANCE, loadBalance);

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
     */
    protected final void selectAttachOrThrow(Invocation invocation, Collection<ServiceInstance> excludes) throws InvokerNotFoundException {
        ReferenceInvoker<T> selected = select(invocation, excludes);

        if (Objects.nonNull(selected)) {
            invocation.attach(ReferenceConstants.SELECTED_INVOKER_KEY, selected);
        } else {
            throw new InvokerNotFoundException(invocation.handler());
        }
    }

    /**
     * invoke filter chain
     *
     * @param invocation rpc call信息
     * @return rpc call result
     */
    protected final RpcResult invokeFilterChain(Invocation invocation) {
        if (log.isDebugEnabled()) {
            log.debug("{} invoke filter chain. invocation={}", getClass().getSimpleName(), invocation);
        }

        try {
            return filterChain.invoke(invocation)
                    .onFinish((r, t) -> {
                        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
                        if (Objects.isNull(methodConfig) || !methodConfig.isSticky()) {
                            return;
                        }

                        //维护服务方法调用invoker sticky
                        if (Objects.isNull(t)) {
                            //rpc call success
                            ReferenceInvoker<T> invoker = invocation.attachment(ReferenceConstants.SELECTED_INVOKER_KEY);
                            if (Objects.nonNull(invoker)) {
                                stickyInvokerCache.put(invocation.handlerId(), invoker);
                            }
                        } else {
                            //rpc call fail
                            stickyInvokerCache.invalidate(invocation.handlerId());
                        }
                    });
        } catch (Exception e) {
            return RpcResult.fail(invocation, e);
        }
    }

    /**
     * 重置或清掉一次rpc call中的临时信息, 用于下次重试时, 重新attach, 避免干扰
     *
     * @param invocation rpc call信息
     */
    protected final void onResetInvocation(Invocation invocation) {
        //保留method config
        MethodConfig methodConfig = invocation.attachment(ReferenceConstants.METHOD_CONFIG_KEY);
        //reset
        invocation.clear();
        //recover retain
        invocation.attach(ReferenceConstants.METHOD_CONFIG_KEY, methodConfig);
    }

    /**
     * 释放占用资源
     */
    public final void destroy() {
        //是否转移到registry unsubscribe更好
        directory.destroy();
    }
}
