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

    /** sender address */
    private Address fromAddress;
    /** receiver actor name */
    private String actorName;
    /** 消息 */
    private Serializable message;

    private transient ActorRef to;
    private transient boolean ignoreResponse;

    /**
     * request
     */
    static MessagePayload request(Address from, ActorRef to, Serializable message, boolean ignoreResponse) {
        MessagePayload payload = new MessagePayload();
        payload.fromAddress = from;
        payload.actorName = to.getAddress().getName();
        payload.message = message;
        payload.to = to;
        payload.ignoreResponse = ignoreResponse;
        return payload;
    }

    /**
     * response
     */
    static MessagePayload response(Address from, Serializable message) {
        MessagePayload payload = new MessagePayload();
        payload.fromAddress = from;
        payload.actorName = "";
        payload.message = message;
        return payload;
    }

    private MessagePayload() {
    }

    //setter && getter
    public Address getFromAddress() {
        return fromAddress;
    }

    public void setFromAddress(Address fromAddress) {
        this.fromAddress = fromAddress;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public Serializable getMessage() {
        return message;
    }

    public void setMessage(Serializable message) {
        this.message = message;
    }

    public Address getToAddress() {
        return to.getAddress().getAddress();
    }

    public boolean isIgnoreResponse() {
        return ignoreResponse;
    }

    @Override
    public String toString() {
        return "RpcMessage{" +
                ", from=" + fromAddress +
                ", actorName=" + actorName +
                ", message=" + message +
                '}';
    }
}
