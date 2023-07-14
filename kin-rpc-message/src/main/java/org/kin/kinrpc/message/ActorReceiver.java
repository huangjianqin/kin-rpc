package org.kin.kinrpc.message;

import org.kin.framework.concurrent.Receiver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2023/7/13
 */
final class ActorReceiver extends Receiver<ActorContext> {
    private static final Logger log = LoggerFactory.getLogger(Actor.class);
    /** actor env */
    private final ActorEnv actorEnv;
    /** real actor */
    private final Actor actor;
    /** message behaviors */
    private final Behaviors behaviors;

    ActorReceiver(ActorEnv actorEnv, Actor actor) {
        this.actorEnv = actorEnv;
        this.actor = actor;
        this.behaviors = actor.createBehaviors();
    }

    @Override
    protected void onStart() {
        super.onStart();
        actor.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
        actor.onStop();
    }

    @Override
    public void receive(ActorContext context) {
        //update thread local actor env
        ActorEnv.update(actorEnv);
        //update thread local actor context
        ActorContext.update(context);
        //update message handle time
        context.setHandleTime(System.currentTimeMillis());

        if (log.isDebugEnabled()) {
            log.debug("receive message from {} to {}, eventTime={}, handleTime={}, message={}",
                    context.getFromActorAddress(),
                    context.getToActorAddress(),
                    context.getEventTime(),
                    context.getHandleTime(),
                    context.getMessage()
            );
        }

        behaviors.onReceive(context.getMessage());
    }
}
