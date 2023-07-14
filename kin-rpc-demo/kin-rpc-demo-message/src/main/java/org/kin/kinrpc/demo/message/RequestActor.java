package org.kin.kinrpc.demo.message;

import org.kin.kinrpc.message.Actor;
import org.kin.kinrpc.message.Behaviors;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class RequestActor extends Actor {
    @Override
    protected void onStart() {
        System.out.println("requester actor start");
    }

    @Override
    protected void onStop() {
        System.out.println("requester actor stop");
    }

    @Override
    protected Behaviors createBehaviors() {
        return Behaviors.builder()
                .interceptors((next, behavior, message) -> {
                    System.out.println("requester intercept behavior, message=" + message);
                    next.intercept(next, behavior, message);
                })
                .behavior(ReplyMessage.class, pm -> {
                    System.out.println("responder reply, " + pm.getContent());
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }
}