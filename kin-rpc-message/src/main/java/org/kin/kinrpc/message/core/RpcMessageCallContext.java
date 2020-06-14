package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.domain.RpcEndpointAddress;
import org.kin.kinrpc.message.transport.protocol.RpcMessage;
import org.kin.kinrpc.transport.domain.RpcAddress;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcMessageCallContext {
    /** rpc环境 */
    private RpcEnv rpcEnv;
    /** sender地址 */
    private RpcAddress fromAddress;
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

    public RpcMessageCallContext(RpcEnv rpcEnv, RpcAddress fromAddress, RpcEndpointRef to, Serializable message, long requestId, long createTime) {
        this.rpcEnv = rpcEnv;
        this.fromAddress = fromAddress;
        this.to = to;
        this.message = message;
        this.requestId = requestId;
        this.createTime = createTime;
    }

    /**
     * 响应客户端请求
     */
    public void reply(Serializable message) {
        RpcMessage rpcMessage =
                new RpcMessage(requestId, to.getEndpointAddress().getRpcAddress(), new RpcEndpointRef(RpcEndpointAddress.of(fromAddress, "")), message);
        rpcEnv.send(rpcMessage);
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
