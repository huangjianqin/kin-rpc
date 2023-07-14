package org.kin.kinrpc.message;

import org.kin.kinrpc.transport.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
final class RemotingActorRef extends ActorRef {
    private static final Logger log = LoggerFactory.getLogger(RemotingActorRef.class);

    /** remoting actor env */
    private final RemotingActorEnv actorEnv;
    //-----------------------------------------------------response actor 才需要有值
    /** remoting request context */
    private RequestContext requestContext;
    /** receiver actor name */
    private String toActorName;

    RemotingActorRef(ActorPath actorPath,
                     RemotingActorEnv actorEnv) {
        super(actorPath);
        this.actorEnv = actorEnv;
    }

    RemotingActorRef(ActorPath actorPath,
                     RemotingActorEnv actorEnv,
                     RequestContext requestContext,
                     String toActorName) {
        super(actorPath);
        this.actorEnv = actorEnv;
        this.requestContext = requestContext;
        this.toActorName = toActorName;
    }

    /**
     * 检查消息是否合法
     *
     * @param message remoting message
     */
    private void checkMessage(Object message) {
        if (message instanceof Serializable) {
            return;
        }

        throw new IllegalArgumentException("remoting message must implement " + Serializable.class.getName());
    }

    @Override
    public final synchronized void answer(Object message) {
        checkMessage(message);

        if (Objects.isNull(requestContext)) {
            log.warn("don not support answer more times, please check logic is right");
            return;
        }

        //限制只能answer一次
        MessagePayload responsePayload =
                MessagePayload.answer(ActorPath.of(actorEnv.getListenAddress(), toActorName), message);
        requestContext.writeMessageResponse(responsePayload);
        requestContext = null;
    }

    @Override
    public void doTell(Object message, ActorRef sender) {
        checkMessage(message);
        MessagePayload messagePayload = MessagePayload.tell(sender.getActorPath(), this, message);
        actorEnv.tell(messagePayload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <R> CompletableFuture<R> doAsk(Object message, ActorRef sender, long timeoutMs) {
        checkMessage(message);
        MessagePayload messagePayload = MessagePayload.ask(sender.getActorPath(), this, message, timeoutMs);
        return (CompletableFuture<R>) actorEnv.ask(messagePayload);
    }
}
