package org.kin.kinrpc.message.core;

import io.netty.channel.Channel;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
import org.kin.kinrpc.transport.kinrpc.KinRpcResponse;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class RpcMessageCallContext {
    /** rpc环境 */
    private RpcEnv rpcEnv;
    /** sender地址 */
    private KinRpcAddress fromAddress;
    /** sender channel */
    private Channel channel;
    /** receiver */
    private RpcEndpointRef to;
    /** 消息 */
    private final Serializable message;
    /** 消息rpc request唯一id */
    private final long requestId;
    /** 消息创建时间 */
    private final long createTime;
    /** 消息事件时间即, 到达receiver端但还未处理的时间 */
    private long eventTime;
    /** 消息处理时间 */
    private long handleTime;

    public RpcMessageCallContext(RpcEnv rpcEnv, KinRpcAddress fromAddress, Channel channel, RpcEndpointRef to, Serializable message, long requestId, long createTime) {
        this.rpcEnv = rpcEnv;
        this.fromAddress = fromAddress;
        this.channel = channel;
        this.to = to;
        this.message = message;
        this.requestId = requestId;
        this.createTime = createTime;
    }

    public RpcMessageCallContext(RpcEnv rpcEnv, KinRpcAddress address, Channel channel, Serializable message) {
        this(rpcEnv, address, channel, null, message, 0, System.currentTimeMillis());
    }

    /**
     * 响应客户端请求
     */
    public void reply(Serializable message) {
        if (Objects.nonNull(channel)) {
            RpcMessage rpcMessage =
                    RpcMessage.of(requestId, to.getEndpointAddress().getRpcAddress(), RpcEndpointRef.of(RpcEndpointAddress.of(fromAddress, "")), message);

            //序列化
            byte[] data = rpcEnv.serialize(rpcMessage);
            if (Objects.isNull(data)) {
                return;
            }

            //直接推回去, 不走outbox
            KinRpcResponse protocol = KinRpcResponse.create(requestId, (byte) rpcEnv.serializer().type(), data);
            channel.writeAndFlush(protocol);
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public KinRpcAddress getFromAddress() {
        return fromAddress;
    }

    public RpcEndpointRef getTo() {
        return to;
    }

    public Serializable getMessage() {
        return message;
    }

    public long getRequestId() {
        return requestId;
    }

    public long getCreateTime() {
        return createTime;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public long getHandleTime() {
        return handleTime;
    }

    public void setHandleTime(long handleTime) {
        this.handleTime = handleTime;
    }
}
