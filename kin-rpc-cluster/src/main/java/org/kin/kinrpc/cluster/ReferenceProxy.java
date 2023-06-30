package org.kin.kinrpc.cluster;

import net.bytebuddy.implementation.bind.annotation.AllArguments;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import org.checkerframework.common.returnsreceiver.qual.This;
import org.eclipse.collections.api.map.primitive.IntObjectMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.MethodHandleUtils;
import org.kin.kinrpc.*;
import org.kin.kinrpc.cluster.call.RpcResultAdapterHelper;
import org.kin.kinrpc.cluster.invoker.ClusterInvoker;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.constants.ReferenceConstants;
import org.kin.kinrpc.utils.GsvUtils;
import org.kin.kinrpc.utils.HandlerUtils;
import org.kin.kinrpc.utils.RpcUtils;
import org.kin.serialization.Serialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * reference代理
 *
 * @author huangjianqin
 * @date 2023/6/25
 */
public final class ReferenceProxy implements InvocationHandler {
    private static final Logger log = LoggerFactory.getLogger(ReferenceProxy.class);

    /** reference端配置 */
    private final ReferenceConfig<?> config;
    /** 序列化code */
    private byte serializationCode;
    /** 服务唯一id */
    private final int serviceId;
    /** 服务唯一标识 */
    private final String service;
    /** cluster invoker */
    private final ClusterInvoker<?> invoker;
    /** 服务方法元数据 */
    private final IntObjectMap<MethodMetadata> methodMetadataMap;
    /** 方法级服务方法配置 */
    private final IntObjectMap<MethodConfig> methodConfigMap;
    /** 服务级服务方法配置 */
    private final MethodConfig globalMethodConfig;

    public ReferenceProxy(ReferenceConfig<?> config, ClusterInvoker<?> invoker) {
        this.config = config;
        this.serializationCode = (byte) ExtensionLoader.getExtensionCode(Serialization.class, config.getSerialization());
        this.service = config.service();
        this.serviceId = GsvUtils.serviceId(this.service);
        this.invoker = invoker;
        this.methodMetadataMap = RpcUtils.getMethodMetadataMap(this.service, config.getInterfaceClass());
        //方法级
        IntObjectHashMap<MethodConfig> methodConfigMap = new IntObjectHashMap<>(config.getMethods().size());
        for (MethodConfig method : config.getMethods()) {
            methodConfigMap.put(HandlerUtils.handlerId(this.service, method.getName()), method);
        }
        this.methodConfigMap = methodConfigMap;
        //服务级
        this.globalMethodConfig = MethodConfig.create("$global")
                .timeout(config.getRpcTimeout())
                .retries(config.getRetries())
                .async(config.isAsync())
                .sticky(config.isSticky());
    }

    @Override
    @RuntimeType
    public Object invoke(@This Object proxy, @Origin Method method, @AllArguments Object[] args) {
        if (Object.class.equals(method.getDeclaringClass())) {
            //过滤Object方法
            try {
                method.invoke(this, args);
            } catch (IllegalAccessException | InvocationTargetException e) {
                ExceptionUtils.throwExt(e);
            }
        }

        if (!KinRpcAppContext.ENHANCE && method.isDefault()) {
            //jdk代理下, 如果是调用default方法, 直接使用句柄调用
            try {
                return MethodHandleUtils.getInterfaceDefaultMethodHandle(method, config.getInterfaceClass()).bindTo(proxy).invokeWithArguments(args);
            } catch (Throwable throwable) {
                ExceptionUtils.throwExt(throwable);
            }
        }

        String uniqueName = RpcUtils.getUniqueName(method);
        int handlerId = HandlerUtils.handlerId(service, uniqueName);
        MethodMetadata methodMetadata = methodMetadataMap.get(handlerId);
        if (Objects.isNull(methodMetadata)) {
            throw new RpcException("can not find valid method metadata for method, " + method);
        }

        MethodConfig methodConfig = getMethodConfig(handlerId);

        RpcInvocation invocation = new RpcInvocation(serviceId,
                service, args, methodMetadata, this.serializationCode);
        invocation.attach(ReferenceConstants.METHOD_CONFIG_KEY, methodConfig);

        if (log.isDebugEnabled()) {
            log.debug("ready to send rpc call. invocation={}", invocation);
        }

        CompletableFuture<Object> userFuture = rpcCall(invocation);
        boolean async = methodConfig.isAsync();
        if (async) {
            //async
            RpcContext.updateFuture(userFuture);
        }

        return RpcResultAdapterHelper.convert(methodMetadata.returnType(), async, userFuture);
    }

    /**
     * 获取method config
     *
     * @param handlerId 服务方法唯一id
     * @return method config
     */
    @Nonnull
    private MethodConfig getMethodConfig(int handlerId) {
        MethodConfig config = methodConfigMap.get(handlerId);
        if (Objects.isNull(config)) {
            config = globalMethodConfig;
        }

        return config;
    }

    /**
     * 发起rpc call
     *
     * @param invocation rpc call信息
     * @return rpc call future
     */
    private CompletableFuture<Object> rpcCall(RpcInvocation invocation) {
        CompletableFuture<Object> userFuture = new CompletableFuture<>();
        // TODO: 2023/6/26 第一次rpc call还是在user invoke线程, 其他是在reference通用线程发起, 真要全异步, 这里需要扔到reference通用线程执行invoker.invoke
        RpcResult rpcResult = invoker.invoke(invocation);
        //reference通用线程处理
        rpcResult.onFinish(userFuture);
        return userFuture;
    }
}