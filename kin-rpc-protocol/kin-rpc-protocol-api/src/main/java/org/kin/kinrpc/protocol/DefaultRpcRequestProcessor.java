package org.kin.kinrpc.protocol;

import io.netty.util.collection.IntCollections;
import io.netty.util.collection.IntObjectHashMap;
import io.netty.util.collection.IntObjectMap;
import org.kin.kinrpc.MethodMetadata;
import org.kin.kinrpc.RpcException;
import org.kin.kinrpc.RpcInvocation;
import org.kin.kinrpc.RpcService;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.InvocationConstants;
import org.kin.kinrpc.transport.RequestContext;
import org.kin.kinrpc.transport.RpcRequestProcessor;
import org.kin.kinrpc.transport.cmd.CodecException;
import org.kin.kinrpc.transport.cmd.RpcRequestCommand;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Objects;

/**
 * 默认rpc request处理实现
 * 管理{@link RpcService}
 *
 * @author huangjianqin
 * @date 2023/6/19
 */
public class DefaultRpcRequestProcessor extends RpcRequestProcessor {
    private static final Logger log = LoggerFactory.getLogger(DefaultRpcRequestProcessor.class);

    /** 已注册的服务元数据 */
    private volatile IntObjectMap<RpcService<?>> rpcServiceMap = IntCollections.emptyMap();

    /**
     * 注册服务
     *
     * @param rpcService 服务
     */
    public synchronized void register(RpcService<?> rpcService) {
        ServiceConfig<?> config = rpcService.getConfig();
        String service = config.getService();
        int serviceId = GsvUtils.serviceId(service);

        //copy
        IntObjectMap<RpcService<?>> rpcServiceMap = new IntObjectHashMap<>(this.rpcServiceMap.size() + 1);
        rpcServiceMap.putAll(this.rpcServiceMap);

        if (rpcServiceMap.containsKey(serviceId)) {
            throw new RpcException(String.format("service '%s' has been registered", service));
        }

        rpcServiceMap.put(serviceId, rpcService);

        //update
        this.rpcServiceMap = rpcServiceMap;
    }

    /**
     * 取消注册服务
     *
     * @param serviceId 服务唯一id
     */
    public synchronized void unregister(int serviceId) {
        //copy
        IntObjectMap<RpcService<?>> rpcServiceMap = new IntObjectHashMap<>(this.rpcServiceMap.size() + 1);
        rpcServiceMap.putAll(this.rpcServiceMap);

        rpcServiceMap.remove(serviceId);

        //update
        this.rpcServiceMap = rpcServiceMap;
    }

    @Override
    public void process(RequestContext requestContext, RpcRequestCommand request) {
        //获取服务元数据
        int serviceId = request.getServiceId();
        RpcService<?> rpcService = rpcServiceMap.get(serviceId);
        if (Objects.isNull(rpcService)) {
            throw new RpcException("can not find service with serviceId=" + serviceId);
        }

        //获取服务方法元数据
        int handlerId = request.getHandlerId();
        MethodMetadata methodMetadata = rpcService.getMethodMetadata(handlerId);
        if (Objects.isNull(methodMetadata)) {
            throw new RpcException("can not find method metadata with handlerId=" + handlerId);
        }

        //反序列化调用参数
        try {
            request.deserializeParams(methodMetadata.paramsType());
        } catch (Exception e) {
            throw new CodecException("deserialize rpc request params fail", e);
        }

        if (request.isTimeout()) {
            //仅仅warning
            log.warn("rpc request timeout before process, request={}", request);
            return;
        }

        //invoke
        Map<String, String> metadata = request.getMetadata();
        RpcInvocation invocation = new RpcInvocation(serviceId, rpcService.service(), rpcService.getInterface(),
                request.getParams(), metadata, methodMetadata);
        invocation.attach(InvocationConstants.TIMEOUT_KEY, request.getTimeout());
        try {
            rpcService.invoke(invocation)
                    .onFinish((r, t) -> onFinish(requestContext, request, invocation.isOneWay(), r, t));
        } catch (Exception e) {
            log.error("process rpc request fail, request= {}", request, e);
            requestContext.writeResponseIfError(new RpcException("process rpc request fail", e));
        }
    }

    /**
     * 服务方法调用
     * !!!一般是在服务调用线程执行, 如果服务方法自定义异步执行逻辑, 那么就会在该异步执行线程执行
     *
     * @param requestContext rpc request context
     * @param request        rpc request
     * @param oneWay         服务方法是否返回void
     * @param result         服务调用结果
     * @param t              服务调用异常
     */
    private void onFinish(RequestContext requestContext,
                          RpcRequestCommand request,
                          boolean oneWay,
                          Object result,
                          Throwable t) {
        if (request.isTimeout()) {
            //仅仅warning
            log.warn("process rpc request timeout after process finish, request={}", request);
            return;
        }

        if (Objects.isNull(t)) {
            //服务调用正常结束
            if (!oneWay) {
                requestContext.writeResponse(result);
            }
        } else {
            //服务调用异常
            t = RpcUtils.normalizeException(t);
            requestContext.writeResponseIfError(t);
        }
    }
}
