package org.kin.kinrpc.demo.message;

import org.kin.framework.JvmCloseCleaner;
import org.kin.kinrpc.message.Actor;
import org.kin.kinrpc.message.ActorEnv;
import org.kin.kinrpc.message.MessagePostContext;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-13
 */
public class ActorDemo extends Actor {
    public ActorDemo(ActorEnv actorEnv) {
        super(actorEnv);
    }

    public static void main(String[] args) {
        ActorEnv actorEnv = new ActorEnv("0.0.0.0", 16888);
        actorEnv.startServer();
        String name = "rpcEndpointDemo";
        ActorDemo rpcEndpointDemo = new ActorDemo(actorEnv);
        actorEnv.newActor(name, rpcEndpointDemo);

        JvmCloseCleaner.instance().add(() -> {
            actorEnv.removeActor(name, rpcEndpointDemo);
            actorEnv.destroy();
        });
    }

    @Override
    protected void onStart() {
        System.out.println("endpoint start");
    }

    @Override
    protected void onStop() {
        System.out.println("endpoint stop");
    }

    @Override
    protected void onReceiveMessage(MessagePostContext context) {
        Serializable message = context.getMessage();
        System.out.println(message);
        if (message instanceof ActorRefDemo.PrintMessage) {
            //相当于创建client, send message
            ActorRefDemo.PrintMessage printMessage = (ActorRefDemo.PrintMessage) message;
            printMessage.getFrom().fireAndForget(new ReplyMessage(context.getRequestId(), Integer.parseInt(printMessage.getContent())));
        } else if (message instanceof ActorRefDemo.AskMessage) {
            //原路返回
            context.response(new ReplyMessage(context.getRequestId(), -1));
        }
    }

    @Override
    public boolean threadSafe() {
        return true;
    }

    public static class ReplyMessage implements Serializable {
        private static final long serialVersionUID = -1586292592951384110L;
        private long requestId;
        private int count;

        public ReplyMessage() {
        }

        public ReplyMessage(long requestId, int count) {
            this.requestId = requestId;
            this.count = count;
        }

        //setter && getter
        public long getRequestId() {
            return requestId;
        }

        public void setRequestId(long requestId) {
            this.requestId = requestId;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        @Override
        public String toString() {
            return "ReplyMessage{" +
                    "requestId='" + requestId + '\'' +
                    '}';
        }
    }
}
