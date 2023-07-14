package org.kin.kinrpc.demo.message;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.message.Actor;
import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.Behaviors;

import java.io.IOException;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class ActorDemo extends Actor {
    public static void main(String[] args) throws IOException {
        ActorEnv actorEnv = ActorEnv.builder().port(16888).build();
        String name = "actorDemo";
        ActorDemo actorDemo = new ActorDemo();
        actorEnv.newActor(name, actorDemo);

        //destroy
        System.in.read();
        actorEnv.removeActor(name, actorDemo);
        actorEnv.destroy();
    }

    @Override
    protected void onStart() {
        System.out.println("actor start");
    }

    @Override
    protected void onStop() {
        System.out.println("actor stop");
    }

    @Override
    protected Behaviors createBehaviors() {
        return Behaviors.builder()
                .interceptors((next, behavior, actorContext, message) -> {
                    System.out.println("actorDemo intercept behavior, message=" + message);
                    next.intercept(next, behavior, actorContext, message);
                })
                .behavior(ActorRefDemo.PrintMessage.class, (ac, pm) -> {
                    System.out.println(pm.getContent());
                    ac.sender().fireAndForget(new ReplyMessage("i have print...." + StringUtils.randomString(ThreadLocalRandom.current().nextInt(20))));
                })
                .behavior(ActorRefDemo.AskMessage.class, (ac, am) -> {
                    //原路返回
                    ac.response(new ReplyMessage("hi, " + am.getContent()));
                })
                .build();
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    public static class ReplyMessage implements Serializable {
        private static final long serialVersionUID = -1586292592951384110L;
        private String content;

        public ReplyMessage() {
        }

        public ReplyMessage(String content) {
            this.content = content;
        }

        //setter && getter
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        @Override
        public String toString() {
            return "ReplyMessage{" +
                    "content='" + content + '\'' +
                    '}';
        }
    }
}
