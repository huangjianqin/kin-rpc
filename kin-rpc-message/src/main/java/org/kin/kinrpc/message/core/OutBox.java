package org.kin.kinrpc.message.core;

import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.transport.kinrpc.KinRpcAddress;
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

    /** 接收方地址 */
    private KinRpcAddress address;
    /** 所属rpc环境 */
    private RpcEnv rpcEnv;
    /** 该OutBox邮箱绑定的client */
    private TransportClient client;
    /** 待发送队列 */
    private LinkedList<OutBoxMessage> pendingMessages = new LinkedList<>();
    private boolean isStopped;
    /** 是否正在发送消息 */
    private boolean draining;
    /** 客户端连接的Future */
    private Future clientConnectFuture;

    OutBox(KinRpcAddress address, RpcEnv rpcEnv) {
        this.address = address;
        this.rpcEnv = rpcEnv;
    }

    /**
     * 发送消息, 本质上是消息入队, 并等待client发送
     */
    void sendMessage(OutBoxMessage outBoxMessage) {
        boolean dropped = false;
        synchronized (this) {
            if (isStopped) {
                dropped = true;
            } else {
                pendingMessages.add(outBoxMessage);
            }
        }

        if (dropped) {
            log.warn("drop message, because of outbox stopped >>>> {}", outBoxMessage);
        } else {
            drainOutbox();
        }
    }

    /**
     * 校验client是否有效
     */
    private boolean validClient() {
        if (Objects.isNull(client)) {
            //没有连接好的客户端, 创建一个
            clientConnect();
            return false;
        }

        if (!client.isActive()) {
            //client inactive
            return false;
        }
        return true;
    }

    /**
     * 消息真正发送的逻辑
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
                TransportClient client;
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
     * 处理消息发送中遇到的异常
     */
    private void handleException(Exception e) {
        log.error("", e);
        synchronized (this) {
            if (Objects.isNull(clientConnectFuture)) {
                //移除该outbox
                rpcEnv.removeOutBox(address);
            }
        }
    }

    /** 获取与该OutBox绑定的client */
    private void clientConnect() {
        clientConnectFuture = rpcEnv.commonExecutors.submit(() -> {
            try {
                TransportClient client = rpcEnv.getClient(address);
                synchronized (OutBox.this) {
                    if (!isStopped) {
                        OutBox.this.client = client;
                        if (OutBox.this.client.isActive()) {
                            //很快连接上
                            rpcEnv.commonExecutors.execute(OutBox.this::drainOutbox);
                        } else {
                            //很慢, 设置callback
                            OutBox.this.client.updateConnectionInitCallback(() -> {
                                rpcEnv.commonExecutors.execute(OutBox.this::drainOutbox);
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
     * outbox stopped
     */
    void stop() {
        synchronized (this) {
            if (isStopped) {
                return;
            }
            while (pendingMessages.size() > 0) {
                //等待所有消息处理完才处理stop逻辑
                try {
                    wait();
                } catch (InterruptedException e) {

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
