package org.kin.kinrpc;

import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.kin.framework.proxy.MethodDefinition;
import org.kin.framework.proxy.Proxys;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.executor.ExecutorManager;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * service端{@link Invoker}实现
 *
 * @author huangjianqin
 * @date 2023/2/27
 */
public class RpcService<T> implements Invoker<T> {
    private static final Logger log = LoggerFactory.getLogger(RpcService.class);

    private final ServiceConfig<T> config;
    /** 服务方法元数据 */
    private final IntObjectMap<RpcHandler> rpcHandlerMap;
    /** 服务调用线程池 */
    private final Executor executor;

    public RpcService(ServiceConfig<T> config) {
        this.config = config;

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

        ExecutorConfig executorConfig = config.getExecutor();
        Executor executor = null;
        if (Objects.nonNull(executorConfig)) {
            executor = ExecutorManager.getOrCreateExecutor(service, executorConfig);
        }
        this.executor = executor;
    }

    @Override
    public RpcResult invoke(Invocation invocation) {
        int handlerId = invocation.handlerId();
        RpcHandler rpcHandler = rpcHandlerMap.get(handlerId);
        if (Objects.isNull(rpcHandler)) {
            throw new IllegalArgumentException("can not find rpc handler with handlerId=" + handlerId);
        }

        CompletableFuture<Object> future = new CompletableFuture<>();
        if (Objects.nonNull(executor)) {
            executor.execute(() -> doInvoke(rpcHandler, invocation, future));
        } else {
            doInvoke(rpcHandler, invocation, future);
        }

        return RpcResult.success(invocation, future);
    }

    /**
     * 服务方法调用并complete {@code future}
     *
     * @param rpcHandler rpc handler
     * @param invocation rpc invocation
     * @param future     result future
     */
    private void doInvoke(RpcHandler rpcHandler,
                          Invocation invocation,
                          CompletableFuture<Object> future) {
        try {
            Object ret = doInvoke0(rpcHandler, invocation);
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
            return CompletableFuture.completedFuture(ret);
        }
    }

    /**
     * 服务方法调用
     *
     * @param rpcHandler rpc handler
     * @param invocation rpc invocation
     */
    private Object doInvoke0(RpcHandler rpcHandler,
                             Invocation invocation) {
        String methodName = invocation.getMethodName();
        Object[] params = invocation.params();

        if (log.isDebugEnabled()) {
            log.debug("handle rpc call... invocation={}", invocation);
        }
        //Object类方法直接调用
        if (invocation.isObjectMethod()) {
            T instance = config.getInstance();
            if ("getClass".equals(methodName)) {
                return instance.getClass();
            } else if ("hashCode".equals(methodName)) {
                return instance.hashCode();
            } else if ("toString".equals(methodName)) {
                return instance.toString();
            } else if ("equals".equals(methodName)) {
                if (params.length == 1) {
                    return instance.equals(params[0]);
                }
                throw new IllegalArgumentException(String.format("method '%s' parameter number error", methodName));
            } else {
                throw new UnsupportedOperationException(String.format("does not support to call method '%s'", methodName));
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
            log.debug("method '{}' actual params' type is {}", methodName, actualParamTypeNames);
        }

        try {
            return rpcHandler.handle(params);
        } catch (Exception e) {
            log.error("method '{}' invoke error, params is {}, {}", methodName, params, e);
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
        return config.service();
    }

    /**
     * 返回服务唯一id
     *
     * @return 服务唯一id
     */
    public int serviceId() {
        return config.serviceId();
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
}
