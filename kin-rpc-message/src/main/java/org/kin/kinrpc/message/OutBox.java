package org.kin.kinrpc.message;

import org.kin.kinrpc.transport.RemotingClient;
import org.kin.kinrpc.transport.RemotingClientStateObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * outbound queue
 * 线程安全
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
final class OutBox {
    private static final Logger log = LoggerFactory.getLogger(OutBox.class);

    /** remote address */
    private final Address address;
    /** actor env */
    private final ActorEnv actorEnv;
    /** remote client */
    private MessageClient client;
    /** 待发送队列 */
    private final LinkedList<OutBoxMessage> pendingMessages = new LinkedList<>();
    private boolean isStopped;
    /** 是否正在发送消息 */
    private volatile boolean draining;
    /** client connect future */
    @SuppressWarnings("rawtypes")
    private Future clientConnectFuture;

    OutBox(Address address, ActorEnv actorEnv) {
        this.address = address;
        this.actorEnv = actorEnv;
    }

    /**
     * 消息入队, 等待client发送
     *
     * @param outBoxMessage message which waiting to send
     */
    void pushMessage(OutBoxMessage outBoxMessage) {
        boolean dropped = false;
        synchronized (this) {
            if (isStopped) {
                dropped = true;
            } else {
                pendingMessages.add(outBoxMessage);
            }
        }

        if (dropped) {
            log.warn("drop message, because of outbox stopped, {}", outBoxMessage);
        } else {
            if (draining) {
                return;
            }

            //async
            actorEnv.commonExecutors.execute(OutBox.this::drainOutbox);
        }
    }

    /**
     * 消息出兑, 即准备发送消息
     */
    private void drainOutbox() {
        OutBoxMessage outBoxMessage;
        synchronized (this) {
            if (isStopped) {
                return;
            }

            if (draining) {
                //有其他线程正在drainOutbox
                return;
            }

            if (Objects.nonNull(clientConnectFuture)) {
                //客户端正在连接服务器
                return;
            }

            if (!validClient()) {
                // TODO: 2021/10/12 如果一致连接不成功, 是否需要移除整个outBox
                return;
            }

            outBoxMessage = pendingMessages.poll();
            notify();
            if (Objects.isNull(outBoxMessage)) {
                return;
            }

            draining = true;
        }

        while (true) {
            try {
                MessageClient client;
                synchronized (this) {
                    if (!validClient()) {
                        return;
                    }

                    client = this.client;
                }
                outBoxMessage.sendWith(client);
            } catch (Exception e) {
                handleException(e);
                return;
            }
            synchronized (this) {
                if (isStopped) {
                    return;
                }

                outBoxMessage = pendingMessages.poll();
                notify();
                if (Objects.isNull(outBoxMessage)) {
                    draining = false;
                    return;
                }
            }
        }
    }

    /**
     * 校验client是否可用
     * 在对象锁内操作
     */
    private boolean validClient() {
        if (Objects.isNull(client)) {
            //没有连接好的客户端, 创建一个
            createClient();
            return false;
        }

        if (!client.isActive()) {
            //client inactive
            return false;
        }
        return true;
    }

    /**
     * 异常统一处理
     * 在对象锁内操作
     *
     * @param e 异常
     */
    private void handleException(Exception e) {
        log.error("", e);
        if (Objects.isNull(clientConnectFuture)) {
            synchronized (this) {
                if (Objects.isNull(clientConnectFuture)) {
                    //client还没连上, 可能是connect fail
                    //移除该outbox
                    actorEnv.removeOutBox(address);
                }
            }
        }
    }

    /**
     * 创建与该OutBox绑定的client
     * 在对象锁内操作
     */
    private void createClient() {
        if (Objects.nonNull(clientConnectFuture)) {
            return;
        }

        clientConnectFuture = actorEnv.commonExecutors.submit(() -> {
            try {
                MessageClient client = actorEnv.getClient(address);
                synchronized (OutBox.this) {
                    if (!isStopped) {
                        OutBox.this.client = client;
                        if (OutBox.this.client.isActive()) {
                            //很快连接上
                            actorEnv.commonExecutors.execute(OutBox.this::drainOutbox);
                        } else {
                            //很慢, 设置callback
                            OutBox.this.client.setClientStateObserver(new RemotingClientStateObserver() {
                                @Override
                                public void onConnectSuccess(RemotingClient client) {
                                    actorEnv.commonExecutors.execute(OutBox.this::drainOutbox);
                                }
                            });
                        }
                    }
                }
            } catch (Exception e) {
                synchronized (OutBox.this) {
                    clientConnectFuture = null;
                }
                handleException(e);
                return;
            }
            synchronized (OutBox.this) {
                clientConnectFuture = null;
            }
            //等待client connected回调
        });
    }

    /**
     * destroy outbox
     */
    void destroy() {
        synchronized (this) {
            if (isStopped) {
                return;
            }
            while (pendingMessages.size() > 0) {
                //等待所有消息处理完才处理stop逻辑
                try {
                    wait();
                } catch (InterruptedException e) {
                    //do nothing
                }
            }
        }
        synchronized (this) {
            if (isStopped) {
                return;
            }

            isStopped = true;
            if (Objects.nonNull(clientConnectFuture)) {
                clientConnectFuture.cancel(true);
            }
            client = null;
        }
    }
}
