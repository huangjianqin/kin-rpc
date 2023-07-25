package org.kin.kinrpc;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.Proxys;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.ServerAttachmentConstants;
import org.kin.kinrpc.executor.ExecutorHelper;
import org.kin.kinrpc.executor.ManagedExecutor;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * rpc服务, 所有服务方法调用最终都会由该类发起调用
 *
 * @author huangjianqin
 * @date 2023/2/27
 */
public class RpcService<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(RpcService.class);

    private final ServiceConfig<T> config;
    private final FilterChain<T> chain;
    /** 服务方法元数据 */
    private final IntObjectMap<RpcHandler> rpcHandlerMap;
    /** 服务调用线程池 */
    private final ManagedExecutor executor;
    /** 是否terminated */
    private volatile boolean terminated;
    /** service token */
    private final String token;

    public RpcService(ServiceConfig<T> config) {
        this.config = config;
        //创建filter chain
        Invoker<T> invoker = this::doInvoke;
        this.chain = FilterChain.create(config, invoker);

        //create rpc handler
        IntObjectHashMap<RpcHandler> rpcHandlerMap = new IntObjectHashMap<>();
        T instance = config.getInstance();
        String service = service();
        int serviceId = serviceId();
        for (MethodMetadata metadata : RpcUtils.getMethodMetadataMap(service, config.getInterfaceClass())) {
            log.info("export service handler, service={}, serviceId={}, handler={}, handlerId={}, method={}",
                    service,
                    serviceId,
                    metadata.handler(),
                    metadata.handlerId(),
                    metadata.method());

            org.kin.framework.proxy.ProxyInvoker<?> proxyInvoker = Proxys.adaptive().enhanceMethod(
                    new MethodDefinition<>(instance, metadata.method()));
            RpcHandler rpcHandler = new RpcHandler(metadata, proxyInvoker);
            rpcHandlerMap.put(metadata.handlerId(), rpcHandler);
        }
        this.rpcHandlerMap = rpcHandlerMap;

        //create invoke executor
        ExecutorConfig executorConfig = config.getExecutor();
        ManagedExecutor executor = null;
        if (Objects.nonNull(executorConfig)) {
            executor = ExecutorHelper.getOrCreateExecutor(executorConfig, service);
        }
        this.executor = executor;

        String token = config.getToken();
        if (StringUtils.isNotBlank(token)) {
            if ("true".equalsIgnoreCase(token)) {
                //自动生成token
                this.token = UUID.randomUUID().toString();
            } else {
                //user配置的token
                this.token = token;
            }
        } else {
            this.token = null;
        }
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        if (isTerminated()) {
            return RpcResult.fail(invocation, new IllegalStateException(String.format("service '%s' unExported", service())));
        }

        try {
            String token = invocation.serverAttachments().remove(ServerAttachmentConstants.TOKEN_KEY);
            if (StringUtils.isNotBlank(this.token) && !this.token.equals(token)) {
                throw new AuthorizationException(String.format("check service '%s' token authorization fail", invocation.service()));
            }

            CompletableFuture<Object> future = new CompletableFuture<>();
            if (Objects.nonNull(executor)) {
                //如果服务执行线程池队列已满, 则抛出RejectedExecutionException, 捕获异常后, 直接返回rpc result
                executor.execute(() -> {
                    if (isTerminated()) {
                        future.complete(new IllegalStateException(String.format("service '%s' unExported", service())));
                        return;
                    }

                    chain.invoke(invocation).onFinish(future);
                });
            } else {
                chain.invoke(invocation).onFinish(future);
            }

            return RpcResult.success(invocation, future);
        } catch (Exception e) {
            //执行异常直接返回
            return RpcResult.fail(invocation, e);
        }
    }

    /**
     * 服务方法调用
     *
     * @param invocation rpc invocation
     * @return rpc result
     */
    private RpcResult doInvoke(Invocation invocation) {
        int handlerId = invocation.handlerId();
        RpcHandler rpcHandler = rpcHandlerMap.get(handlerId);
        if (Objects.isNull(rpcHandler)) {
            throw new IllegalArgumentException("can not find rpc handler with handlerId=" + handlerId);
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        doInvoke0(rpcHandler, invocation, future);
        return RpcResult.success(invocation, future);
    }

    /**
     * 服务方法调用并complete {@code future}
     *
     * @param rpcHandler rpc handler
     * @param invocation rpc invocation
     * @param future     result future
     */
    private void doInvoke0(RpcHandler rpcHandler,
                           Invocation invocation,
                           CompletableFuture<Object> future) {
        try {
            //关联rpc context与attachments
            RpcContext.attachMany(invocation.serverAttachments());
            Object ret = doInvoke1(rpcHandler, invocation);
            CompletableFuture<Object> invokeFuture = wrapFuture(ret);
            invokeFuture.whenComplete((r, t) -> {
                if (Objects.isNull(t)) {
                    //invoke success
                    future.complete(r);
                } else {
                    future.completeExceptionally(t);
                }
            });
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
    }

    /**
     * 将服务调用结果封装成{@link CompletableFuture}
     */
    @SuppressWarnings("unchecked")
    private CompletableFuture<Object> wrapFuture(Object ret) {
        if (ret instanceof CompletableFuture) {
            return (CompletableFuture<Object>) ret;
        } else if (ret instanceof Mono) {
            return (CompletableFuture<Object>) ((Mono<?>) ret).toFuture();
        } else {
            //非异步返回结果
            AsyncContext asyncContext = AsyncContext.remove();
            if (Objects.nonNull(asyncContext)) {
                //使用了async context
                return asyncContext.getFuture();
            } else {
                return CompletableFuture.completedFuture(ret);
            }
        }
    }

    /**
     * 服务方法调用
     *
     * @param rpcHandler rpc handler
     * @param invocation rpc invocation
     */
    private Object doInvoke1(RpcHandler rpcHandler,
                             Invocation invocation) {
        String handlerName = invocation.handlerName();
        Object[] params = invocation.params();

        if (log.isDebugEnabled()) {
            log.debug("handle rpc call. invocation={}", invocation);
        }
        //Object类方法直接调用
        if (invocation.isObjectMethod()) {
            T instance = config.getInstance();
            if ("getClass".equals(handlerName)) {
                return instance.getClass();
            } else if ("hashCode".equals(handlerName)) {
                return instance.hashCode();
            } else if ("toString".equals(handlerName)) {
                return instance.toString();
            } else if ("equals".equals(handlerName)) {
                if (params.length == 1) {
                    return instance.equals(params[0]);
                }
                throw new IllegalArgumentException(String.format("method '%s' parameter number error", handlerName));
            } else {
                throw new UnsupportedOperationException(String.format("does not support to call method '%s'", handlerName));
            }
        }

        //其他方法
        //打印入参信息
        int paramLength = params == null ? 0 : params.length;
        String[] actualParamTypeNames = new String[paramLength];
        for (int i = 0; i < actualParamTypeNames.length; i++) {
            actualParamTypeNames[i] = params[i].getClass().getName();
        }
        if (log.isDebugEnabled()) {
            log.debug("method '{}' actual params' type is {}", handlerName, actualParamTypeNames);
        }

        try {
            return rpcHandler.handle(params);
        } catch (Exception e) {
            log.error("method '{}' invoke error, params is {}", handlerName, params, e);
            ExceptionUtils.throwExt(e);
            return null;
        }
    }

    /**
     * 返回服务唯一标识
     *
     * @return 服务唯一标识
     */
    public String service() {
        return config.getService();
    }

    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id
     */
    public int serviceId() {
        return config.getServiceId();
    }

    /**
     * 释放占用资源
     */
    public void destroy() {
        if (isTerminated()) {
            return;
        }
        terminated = true;
        if (Objects.nonNull(executor)) {
            executor.shutdown();
        }
    }

    //getter
    public ServiceConfig<T> getConfig() {
        return config;
    }

    public Class<T> getInterface() {
        return config.getInterfaceClass();
    }

    /**
     * 返回服务方法元数据
     *
     * @param handlerId 服务方法唯一id
     * @return 服务方法元数据
     */
    @Nullable
    public MethodMetadata getMethodMetadata(int handlerId) {
        RpcHandler rpcHandler = rpcHandlerMap.get(handlerId);
        if (Objects.nonNull(rpcHandler)) {
            return rpcHandler.metadata();
        }
        return null;
    }

    /**
     * 返回所有已注册的服务方法元数据
     *
     * @return 服务方法元数据集合
     */
    public Collection<MethodMetadata> getMethodMetadatas() {
        return rpcHandlerMap.values().stream().map(RpcHandler::metadata).collect(Collectors.toList());
    }

    public boolean isTerminated() {
        return terminated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RpcService)) return false;
        RpcService<?> that = (RpcService<?>) o;
        return Objects.equals(config.getService(), that.config.getService());
    }

    @Override
    public int hashCode() {
        return Objects.hash(config.getService());
    }
}
