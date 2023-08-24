package org.kin.kinrpc.cluster.invoker;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionException;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.SPI;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.InvokerNotFoundException;
import org.kin.kinrpc.cluster.loadbalance.LoadBalance;
import org.kin.kinrpc.cluster.router.Router;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.registry.directory.Directory;
import org.kin.kinrpc.utils.ReferenceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 特殊的{@link ReferenceInvoker}实现, 不是对单一服务实例发起RPC请求, 而是有策略地从服务实例集群挑选一个服务实例并发起RPC请求
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
@SPI(alias = "cluster", singleton = false)
public abstract class ClusterInvoker<T> implements ReferenceInvoker<T> {
    private static final Logger log = LoggerFactory.getLogger(ClusterInvoker.class);

    /** reference config */
    protected final ReferenceConfig<T> referenceConfig;
    /** registry config */
    private final RegistryConfig registryConfig;
    /** 管理订阅服务的所有invoker实例 */
    protected final Directory directory;
    /** cluster伪装的服务实例 */
    private final ServiceInstance serviceInstance;
    /** 路由策略 */
    protected final Router router;
    /** 负载均衡策略 */
    protected final LoadBalance loadBalance;
    /** filter chain */
    private final FilterChain<T> filterChain;
    /** key -> 服务方法唯一id, 即handlerId, value -> 上一次服务调用成功的invoker */
    private final Cache<Integer, ReferenceInvoker<T>> stickyInvokerCache = CacheBuilder.newBuilder()
            //5分钟内没有任何访问即移除
            .expireAfterAccess(Duration.ofMinutes(5))
            .build();

    @SuppressWarnings("unchecked")
    protected ClusterInvoker(ReferenceConfig<T> referenceConfig,
                             @Nullable RegistryConfig registryConfig,
                             Directory directory) {
        this.referenceConfig = referenceConfig;
        this.registryConfig = registryConfig;
        //服务实例变化, 马上清掉stick invoker cache
        this.directory = directory;
        this.directory.addListener((sis) -> stickyInvokerCache.cleanUp());
        if (Objects.nonNull(registryConfig)) {
            this.serviceInstance = new ClusterServiceInstance(referenceConfig.getServiceId(), referenceConfig.getService(),
                    registryConfig.getType(), registryConfig.getAddress(), registryConfig.getWeight());
        } else {
            //多注册中心场景(ZoneAwareClusterInvoker -> multi cluster invoker) or cluster invoker wrap cluster invokers
            this.serviceInstance = new ClusterServiceInstance(referenceConfig.getServiceId(), referenceConfig.getService(),
                    "MultiCluster");
        }

        List<Filter> filters = new ArrayList<>(referenceConfig.getFilters());
        filters.addAll(ReferenceUtils.getReferenceFilters());

        //创建filter chain
        this.filterChain = (FilterChain<T>) FilterChain.create(ReferenceUtils.internalPreFilters(),
                filters,
                ReferenceUtils.internalPostFilters(),
                RpcCallInvoker.instance());

        //创建loadbalance
        this.loadBalance = ExtensionLoader.getExtension(LoadBalance.class, referenceConfig.getLoadBalance());
        if (Objects.isNull(this.loadBalance)) {
            throw new ExtensionException(String.format("can not find loadbalance named '%s', please check whether related SPI config is missing", referenceConfig.getLoadBalance()));
        }

        //创建router
        this.router = ExtensionLoader.getExtension(Router.class, referenceConfig.getRouter());
        if (Objects.isNull(this.router)) {
            throw new ExtensionException(String.format("can not find router named '%s', please check whether related SPI config is missing", referenceConfig.getRouter()));
        }
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
        MethodConfig methodConfig = invocation.attachment(InvocationConstants.METHOD_CONFIG_KEY);
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
        invocation.attach(InvocationConstants.LOADBALANCE_KEY, loadBalance);

        if (log.isDebugEnabled()) {
            if (loadBalancedInvoker != null) {
                log.debug("reference invoker select finished, selected={}, invocation={}", loadBalancedInvoker.serviceInstance(), invocation);
            } else {
                log.debug("can not select any available invoker, invocation={}", invocation);
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
            invocation.attach(InvocationConstants.SELECTED_INVOKER_KEY, selected);
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
            CompletableFuture<Object> filterChainInvokeFuture = new CompletableFuture<>();
            RpcResult filterChainInvokeResult = filterChain.invoke(invocation)
                    .onFinish((r, t) -> onFilterChainInvokeFinish(invocation, r, t, filterChainInvokeFuture));
            return RpcResult.success(invocation, filterChainInvokeFuture);
        } catch (Exception e) {
            return RpcResult.fail(invocation, e);
        }
    }

    /**
     * call after filter chain invoke finish
     *
     * @param invocation              rpc call信息
     * @param result                  rpc call result
     * @param t                       rpc call exception
     * @param filterChainInvokeFuture filter chain invoke listen future
     */
    private void onFilterChainInvokeFinish(Invocation invocation,
                                           @Nullable Object result,
                                           @Nullable Throwable t,
                                           CompletableFuture<Object> filterChainInvokeFuture) {
        MethodConfig methodConfig = invocation.attachment(InvocationConstants.METHOD_CONFIG_KEY);
        if (Objects.isNull(methodConfig)) {
            return;
        }

        ReferenceInvoker<T> invoker = invocation.attachment(InvocationConstants.SELECTED_INVOKER_KEY);
        if (methodConfig.isSticky()) {
            //sticky method call
            //维护sticky
            if (Objects.isNull(t)) {
                //rpc call success
                if (Objects.nonNull(invoker)) {
                    stickyInvokerCache.put(invocation.handlerId(), invoker);
                }
            } else {
                //rpc call fail
                stickyInvokerCache.invalidate(invocation.handlerId());
            }
        }

        //rpc call profile
        invocation.attach(InvocationConstants.RPC_CALL_END_TIME_KEY, System.currentTimeMillis());

        RpcResponse rpcResponse = new RpcResponse(result, t);
        filterChain.onResponse(invocation, rpcResponse);

        //overwrite
        result = rpcResponse.getResult();
        t = rpcResponse.getException();
        if (Objects.isNull(t)) {
            filterChainInvokeFuture.complete(result);
        } else {
            filterChainInvokeFuture.completeExceptionally(t);
        }
    }

    /**
     * 重置或清掉一次rpc call中的临时信息, 用于下次重试时, 重新attach, 避免干扰
     *
     * @param invocation rpc call信息
     */
    protected final void onResetInvocation(Invocation invocation) {
        invocation.detach(InvocationConstants.SELECTED_INVOKER_KEY);
        invocation.detach(InvocationConstants.LOADBALANCE_KEY);
        invocation.detach(InvocationConstants.FILTER_CHAIN_KEY);
    }

    @Override
    public final ServiceInstance serviceInstance() {
        return serviceInstance;
    }

    @Override
    public final boolean isAvailable() {
        return directory.isAvailable();
    }

    /**
     * 返回是否是优先选择的cluster
     *
     * @return true表示是优先选择的cluster
     */
    public boolean isPreferred() {
        return Objects.nonNull(registryConfig) && registryConfig.isPreferred();
    }

    /**
     * 返回cluster zone
     *
     * @return cluster zone
     */
    public String getZone() {
        return Objects.nonNull(registryConfig) ? registryConfig.getZone() : "false";
    }

    public RegistryConfig getRegistryConfig() {
        return registryConfig;
    }

    /**
     * 释放占用资源
     */
    @Override
    public final void destroy() {
        directory.destroy();
    }
}
