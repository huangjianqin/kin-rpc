package org.kin.kinrpc.message.core;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.transport.kinrpc.KinRpcRequestIdGenerator;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * rpc message client
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class RpcEndpointRef implements Serializable {
    private static final long serialVersionUID = 3191956547695414179L;
    /** remote address */
    private RpcEndpointAddress endpointAddress;
    /** rpc环境 */
    private transient volatile RpcEnv rpcEnv;

    public RpcEndpointRef() {
        //更新local RpcEnv
        rpcEnv = RpcEnv.currentRpcEnv();
    }

    public static RpcEndpointRef of(RpcEndpointAddress endpointAddress) {
        RpcEndpointRef rpcEndpointRef = new RpcEndpointRef();
        rpcEndpointRef.endpointAddress = endpointAddress;
        return rpcEndpointRef;
    }

    public static RpcEndpointRef of(RpcEndpointAddress endpointAddress, RpcEnv rpcEnv) {
        RpcEndpointRef rpcEndpointRef = RpcEndpointRef.of(endpointAddress);
        rpcEndpointRef.rpcEnv = rpcEnv;
        return rpcEndpointRef;
    }

    /**
     * 获取rpc环境, 如果实例成员变量没有赋值, 则从ThreadLocal获取
     */
    private RpcEnv rpcEnv() {
        //反序列化时没有获取到RpcEnv, 则尝试从执行线程获取RpcEnv
        if (Objects.isNull(rpcEnv)) {
            synchronized (this) {
                if (Objects.isNull(rpcEnv)) {
                    rpcEnv = RpcEnv.currentRpcEnv();
                }
            }
        }
        return rpcEnv;
    }

    /**
     * 封装成RpcMessage
     */
    private RpcMessage rpcMessage(Serializable message) {
        return RpcMessage.of(KinRpcRequestIdGenerator.next(), rpcEnv().address(), this, message);
    }

    /**
     * 发送消息
     */
    public void fireAndForget(Serializable message) {
        rpcEnv().fireAndForget(rpcMessage(message));
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完并返回
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message) {
        return rpcEnv().requestResponse(rpcMessage(message));
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完并返回, 并且支持超时
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, long timeoutMs) {
        return rpcEnv().requestResponse(rpcMessage(message), timeoutMs);
    }

    /**
     * 发送消息, 并返回Future, 支持阻塞等待待消息处理完并返回, 并且支持超时
     */
    public <R extends Serializable> CompletableFuture<R> requestResponse(Serializable message, long timeout, TimeUnit unit) {
        return requestResponse(message, unit.toMillis(timeout));
    }

    /**
     * 发送消息, 响应时触发callback, 并且支持超时
     *
     * @param callback 自定义callback
     */
    public void requestResponse(Serializable message, RpcCallback callback) {
        Preconditions.checkNotNull(callback);
        rpcEnv().requestResponse(rpcMessage(message), callback);
    }

    /**
     * 发送消息, 响应时触发callback, 并且支持超时
     *
     * @param callback  自定义callback
     * @param timeoutMs 超时时间
     */
    public void requestResponse(Serializable message, RpcCallback callback, long timeoutMs) {
        Preconditions.checkNotNull(callback);
        rpcEnv().requestResponse(rpcMessage(message), callback, timeoutMs);
    }

    /**
     * 发送消息, 响应时触发callback, 并且支持超时
     *
     * @param callback  自定义callback
     * @param timeoutMs 超时时间
     */
    public void requestResponse(Serializable message, RpcCallback callback, long timeoutMs, TimeUnit unit) {
        requestResponse(message, callback, unit.toMillis(timeoutMs));
    }

    //getter
    public RpcEndpointAddress getEndpointAddress() {
        return endpointAddress;
    }

    @Override
    public String toString() {
        return "RpcEndpointRef{" +
                "endpointAddress=" + endpointAddress +
                '}';
    }
}
