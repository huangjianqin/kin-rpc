package org.kin.kinrpc.demo.message;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.Actor;
import org.kin.kinrpc.message.Behaviors;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2023/7/14
 */
public class ResponderActor extends Actor {
    @Override
    protected void onStart() {
        System.out.println("responder actor start");
    }

    @Override
    protected void onStop() {
        System.out.println("responder actor stop");
    }

    @Override
    protected Behaviors createBehaviors() {
        return Behaviors.builder()
                .interceptors((next, behavior, message) -> {
                    System.out.println("responder intercept behavior, message=" + message);
                    next.intercept(next, behavior, message);
                })
                .behavior(PrintMessage.class, pm -> {
                    System.out.println(pm.getContent());
                    sender().tell(new ReplyMessage("i have print...." + StringUtils.randomString(ThreadLocalRandom.current().nextInt(20))));
                })
                .behavior(AskMessage.class, am -> {
                    //原路返回
                    sender().answer(new ReplyMessage("hi, " + am.getContent()));
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }
}