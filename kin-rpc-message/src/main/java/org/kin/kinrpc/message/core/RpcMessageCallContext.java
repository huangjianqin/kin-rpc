package org.kin.kinrpc.message.core;

import io.netty.channel.Channel;
import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcMessageCallContext {
    /** rpc环境 */
    private RpcEnv rpcEnv;
    /** sender地址 */
    private RpcAddress fromAddress;
    private Channel channel;
    /** receive */
    private RpcEndpointRef to;
    /** 消息 */
    private final Serializable message;
    /** request唯一id */
    private final long requestId;
    /** request创建时间 */
    private final long createTime;
    /** request事件时间即, 到达service端的时间 */
    private long eventTime;
    /** request处理时间 */
    private long handleTime;

    public RpcMessageCallContext(RpcEnv rpcEnv, RpcAddress fromAddress, Channel channel, RpcEndpointRef to, Serializable message, long requestId, long createTime) {
        this.rpcEnv = rpcEnv;
        this.fromAddress = fromAddress;
        this.channel = channel;
        this.to = to;
        this.message = message;
        this.requestId = requestId;
        this.createTime = createTime;
    }

    /**
     * 响应客户端请求
     */
    public void reply(Serializable message) {
        if (Objects.nonNull(channel)) {
            RpcMessage rpcMessage =
                    new RpcMessage(requestId, to.getEndpointAddress().getRpcAddress(), new RpcEndpointRef(RpcEndpointAddress.of(fromAddress, "")), message);

            byte[] data = rpcEnv.serialize(rpcMessage);
            if (Objects.isNull(data)) {
                return;
            }

            RpcResponseProtocol protocol = RpcResponseProtocol.create(data);
            channel.writeAndFlush(protocol.write());
        }
    }

    //------------------------------------------------------------------------------------------------------------------
    public RpcAddress getFromAddress() {
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
