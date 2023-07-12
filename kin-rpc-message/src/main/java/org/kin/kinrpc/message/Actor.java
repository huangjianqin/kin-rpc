package org.kin.kinrpc.message;

import org.kin.framework.concurrent.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;

/**
 * actor抽象
 *
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class Actor extends Receiver<MessagePostContext> {
    private static final Logger log = LoggerFactory.getLogger(Actor.class);
    /** rpc环境 */
    protected final ActorEnv actorEnv;

    public Actor(ActorEnv actorEnv) {
        this.actorEnv = actorEnv;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    public final void receive(MessagePostContext context) {
        //update thread local actor env
        ActorEnv.update(actorEnv);
        //update message handle time
        context.setHandleTime(System.currentTimeMillis());

        if (log.isDebugEnabled()) {
            log.debug("receive message from {} to {}, {}-{}-{}-{}",
                    context.getFromAddress(),
                    Objects.nonNull(to) ? to.getAddress().getName() : "internal",
                    context.getCreateTime(),
                    context.getEventTime(),
                    context.getHandleTime(),
                    context.getMessage()
            );
        }

        onReceive(context, context.getMessage());
    }

    /**
     * user定义消息处理逻辑实现
     */
    protected abstract void onReceive(MessagePostContext context, Serializable message);

    /**
     * 标识是否线程安全actor
     */
    public boolean threadSafe() {
        return false;
    }

    /**
     * 返回该actor对应的reference
     */
    public final ActorRef ref() {
        return actorEnv.actorOf(this);
    }

    /**
     * 给自己发送消息
     */
    public final void send2Self(Serializable message) {
        actorEnv.postMessage(MessagePayload.request(actorEnv.getListenAddress(), actorEnv.actorOf(this), message, true));
    }
}
