package org.kin.kinrpc.message.api;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public class RpcMessageCallContext {
    /** sender */
    private RpcEndpointRef from;
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

    public RpcMessageCallContext(RpcEndpointRef from, Serializable message, long requestId, long createTime) {
        this.from = from;
        this.message = message;
        this.requestId = requestId;
        this.createTime = createTime;
    }

    /**
     * 响应客户端请求
     */
    public void reply(RpcEndpointRef replier, Serializable message) {
        from.send(replier, message);
    }

    //------------------------------------------------------------------------------------------------------------------
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
