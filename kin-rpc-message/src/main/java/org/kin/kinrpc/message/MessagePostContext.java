package org.kin.kinrpc.message;

import org.kin.kinrpc.transport.RequestContext;

import java.io.Serializable;
import java.util.Objects;

/**
 * 消息处理上下文
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class MessagePostContext {
    /** actor env */
    private final ActorEnv actorEnv;
    /** sender address */
    private final Address fromAddress;
    /**
     * request context
     * send local message的时候, 为null
     */
    private final RequestContext requestContext;
    /** 消息 */
    private final Serializable message;
    // TODO: 2023/7/12
    /** 消息创建时间 */
    private long createTime;
    /** 消息事件时间, 即到达receiver端但还未处理的时间 */
    private long eventTime;
    /** 消息处理时间 */
    private long handleTime;

    public MessagePostContext(ActorEnv actorEnv, RequestContext requestContext, Address fromAddress, Serializable message) {
        this.actorEnv = actorEnv;
        this.requestContext = requestContext;
        this.fromAddress = fromAddress;
        this.message = message;
    }

    /** 原路返回, 响应客户端请求 */
    public void response(Serializable message) {
        if (Objects.isNull(requestContext)) {
            return;
        }
        MessagePayload responsePayload =
                MessagePayload.response(actorEnv.getListenAddress(), message);
        requestContext.writeMessageResponse(responsePayload);
    }

    public ActorRef sender() {
        return actorEnv.actorOf()
    }

    //------------------------------------------------------------------------------------------------------------------
    public Address getFromAddress() {
        return fromAddress;
    }

    public Serializable getMessage() {
        return message;
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
