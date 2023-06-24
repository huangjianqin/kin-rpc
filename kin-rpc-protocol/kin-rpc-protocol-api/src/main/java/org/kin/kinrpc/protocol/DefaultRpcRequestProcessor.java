package org.kin.kinrpc.protocol;

import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import org.kin.kinrpc.*;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.executor.ExecutorManager;
import org.kin.kinrpc.transport.RequestContext;
import org.kin.kinrpc.transport.RpcRequestProcessor;
import org.kin.kinrpc.transport.cmd.CodecException;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.utils.GsvUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executor;

/**
 * @author huangjianqin
 * @date 2023/6/19
 */
public class DefaultRpcRequestProcessor extends RpcRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(DefaultRpcRequestProcessor.class);

    /** 已注册的服务元数据 */
    private final IntObjectMap<ServiceMetadata> serviceMetadataMap = new IntObjectHashMap<>(16);

    /**
     * 注册服务invoker
     *
     * @param invoker 服务invoker
     */
    public synchronized void register(ServiceInvoker<?> invoker) {
        ServiceConfig<?> config = invoker.getConfig();
        String gsv = config.gsv();
        int serviceId = GsvUtils.serviceId(gsv);
        if (serviceMetadataMap.containsKey(serviceId)) {
            throw new RpcException("service invoker has been registered, gsv = " + gsv);
        }

        ExecutorConfig executorConfig = config.getExecutor();
        Executor executor = null;
        if (Objects.nonNull(executorConfig)) {
            executor = ExecutorManager.getOrCreateExecutor(gsv, executorConfig);
        }
        serviceMetadataMap.put(serviceId, new ServiceMetadata(config, invoker, executor));
    }

    @Override
    public void process(RequestContext requestContext, RpcRequestCommand request) {
        //获取服务元数据
        String gsv = request.getGsv();
        int serviceId = GsvUtils.serviceId(gsv);
        ServiceMetadata serviceMetadata = serviceMetadataMap.get(serviceId);
        if (Objects.isNull(serviceMetadata)) {
            throw new RpcException("can not find service metadata with serviceId " + serviceId);
        }
        //获取服务方法元数据
        String methodName = request.getMethod();
        MethodMetadata methodMetadata = serviceMetadata.getMethodMetadata(methodName);
        if (Objects.isNull(methodMetadata)) {
            throw new RpcException(String.format("can not find methodName '%s' metadata", methodName));
        }

        //反序列化调用参数
        try {
            request.deserializeParams(methodMetadata.getParamsType());
        } catch (Exception e) {
            throw new CodecException("deserialize rpc request params fail", e);
        }

        //invoke
        RpcInvocation invocation = new RpcInvocation(serviceId, gsv, request.getParams(), methodMetadata);
        Invoker<?> invoker = serviceMetadata.getInvoker();
        Executor executor = serviceMetadata.getExecutor();
        if (Objects.nonNull(executor)) {
            executor.execute(() -> doProcess(requestContext, request, invoker, invocation));
        } else {
            doProcess(requestContext, request, invoker, invocation);
        }
    }

    /**
     * 服务方法调用
     *
     * @param requestContext rpc request context
     * @param request        rpc request
     * @param invoker        service invoker
     * @param invocation     rpc invocation
     */
    private void doProcess(RequestContext requestContext,
                           RpcRequestCommand request,
                           Invoker<?> invoker,
                           RpcInvocation invocation) {
        try {
            RpcResult rpcResult = invoker.invoke(invocation);
            if (!invocation.isOneWay()) {
                rpcResult.onFinish((r, t) -> onFinish(requestContext, request, r, t));
            }
        } catch (Exception e) {
            log.error("process rpc request fail, request= {}", request, e);
            requestContext.writeResponseIfError(new RpcException("process rpc request fail", e));
        }
    }

    /**
     * 服务方法调用
     *
     * @param requestContext rpc request context
     * @param result         服务调用结果
     * @param t              服务调用异常
     */
    private void onFinish(RequestContext requestContext,
                          RpcRequestCommand request,
                          Object result,
                          Throwable t) {
        if (request.isTimeout()) {
            //仅仅warning
            log.warn("process rpc request timeout, request={}", request);
        }

        if (Objects.isNull(t)) {
            //服务调用正常结束
            requestContext.writeResponse(result);
        } else {
            //服务调用异常
            requestContext.writeResponseIfError(t);
        }
    }
}
