package org.kin.kinrpc.message;

import org.kin.framework.collection.AttachmentMap;
import org.kin.kinrpc.transport.RequestContext;

import java.io.Serializable;
import java.util.Objects;

/**
 * 消息处理上下文
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public final class ActorContext extends AttachmentMap {
    /** actor env */
    private final ActorEnv actorEnv;
    /** sender address */
    private final ActorAddress fromActorAddress;
    /** receiver actor name */
    private String toActorName;
    /**
     * request context
     * send local message的时候, 为null
     */
    private final RequestContext requestContext;
    /** 消息 */
    private final Serializable message;
    /** 消息事件时间, 即到达receiver端但还未处理的时间 */
    private long eventTime;
    /** 消息处理时间 */
    private long handleTime;

    public ActorContext(ActorEnv actorEnv, RequestContext requestContext, MessagePayload payload) {
        this.actorEnv = actorEnv;
        this.requestContext = requestContext;
        this.fromActorAddress = payload.getFromActorAddress();
        this.toActorName = payload.getToActorName();
        this.message = payload.getMessage();
    }

    /** 原路返回, 响应客户端请求 */
    public void response(Serializable message) {
        if (Objects.isNull(requestContext)) {
            return;
        }
        MessagePayload responsePayload =
                MessagePayload.response(ActorAddress.of(actorEnv.getListenAddress(), toActorName), message);
        requestContext.writeMessageResponse(responsePayload);
    }

    /**
     * 返回message sender actor
     *
     * @return message sender actor
     */
    public ActorRef sender() {
        return ActorRef.of(fromActorAddress, actorEnv);
    }

    //setter && getter
    public ActorAddress getFromActorAddress() {
        return fromActorAddress;
    }

    public Serializable getMessage() {
        return message;
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

    public String getToActorName() {
        return toActorName;
    }
}
