package org.kin.kinrpc.message;

import java.io.Serializable;

/**
 * actor间消息通信payload
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
final class MessagePayload implements Serializable {
    private static final long serialVersionUID = -7580281019273609173L;

    /** sender actor address */
    private ActorAddress fromActorAddress;
    /** receiver actor name */
    private String toActorName;
    /** 请求超时结束时间 */
    private long timeout;
    /** 消息 */
    private Object message;

    //----------------------------------------------tmp
    /** receiver actor */
    private transient ActorRef to;
    /** 是否忽略返回值, 即fire and forget */
    private transient boolean ignoreResponse;

    /**
     * request ignore response
     */
    static MessagePayload tell(ActorAddress fromActorAddress, ActorRef to, Object message) {
        return ask(fromActorAddress, to, message, true, 0);
    }

    /**
     * request
     */
    static MessagePayload ask(ActorAddress fromActorAddress, ActorRef to, Object message, long timeout) {
        return ask(fromActorAddress, to, message, false, timeout);
    }

    /**
     * request
     */
    private static MessagePayload ask(ActorAddress fromActorAddress, ActorRef to, Object message, boolean ignoreResponse, long timeout) {
        MessagePayload payload = new MessagePayload();
        payload.fromActorAddress = fromActorAddress;
        payload.toActorName = to.getActorAddress().getName();
        payload.timeout = timeout;
        payload.message = message;
        payload.to = to;
        payload.ignoreResponse = ignoreResponse;
        return payload;
    }

    /**
     * response
     */
    static MessagePayload answer(ActorAddress fromActorAddress, Object message) {
        MessagePayload payload = new MessagePayload();
        payload.fromActorAddress = fromActorAddress;
        payload.toActorName = "";
        payload.message = message;
        return payload;
    }

    private MessagePayload() {
    }

    //setter && getter
    public ActorAddress getFromActorAddress() {
        return fromActorAddress;
    }

    public void setFromActorAddress(ActorAddress fromActorAddress) {
        this.fromActorAddress = fromActorAddress;
    }

    public String getToActorName() {
        return toActorName;
    }

    public void setToActorName(String toActorName) {
        this.toActorName = toActorName;
    }

    public Object getMessage() {
        return message;
    }

    public void setMessage(Object message) {
        this.message = message;
    }

    public Address getToAddress() {
        return to.getActorAddress().getAddress();
    }

    public boolean isIgnoreResponse() {
        return ignoreResponse;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "MessagePayload{" +
                "fromActorAddress=" + fromActorAddress +
                ", toActorName='" + toActorName + '\'' +
                ", message=" + message +
                ", to=" + to +
                ", ignoreResponse=" + ignoreResponse +
                ", timeout=" + timeout +
                '}';
    }
}
