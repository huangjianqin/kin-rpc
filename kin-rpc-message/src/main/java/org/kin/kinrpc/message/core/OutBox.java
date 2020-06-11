package org.kin.kinrpc.message.core;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.message.transport.TransportClient;
import org.kin.kinrpc.transport.domain.RpcAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.Objects;
import java.util.concurrent.Future;

/**
 * 线程安全
 *
 * @author huangjianqin
 * @date 2020-06-10
 */
public class OutBox {
    private static final Logger log = LoggerFactory.getLogger(OutBox.class);

    /** 接收方地址 */
    private RpcAddress address;
    /** 所属rpc环境 */
    private RpcEnv rpcEnv;
    /** 该OutBox绑定的邮箱 */
    private TransportClient client;
    /** 待发送队列 */
    private LinkedList<OutBoxMessage> pendingMessages = new LinkedList<>();
    private boolean isStopped;
    /** 是否正在发送消息 */
    private boolean draining;
    /** 客户端连接的Future */
    private Future clientConnectFuture;

    public OutBox(RpcAddress address, RpcEnv rpcEnv) {
        this.address = address;
        this.rpcEnv = rpcEnv;
    }

    /**
     * 发送消息, 本质上是消息入队, 并等待client发送
     */
    public void sendMessage(OutBoxMessage outBoxMessage) {
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
     * 消息真正发送的逻辑
     */
    private void drainOutbox() {
        OutBoxMessage outBoxMessage;
        synchronized (this) {
            if (isStopped) {
                return;
            }

            if (Objects.nonNull(clientConnectFuture)) {
                //客户端正在连接服务器
                return;
            }

            if (Objects.isNull(client)) {
                //没有连接好的客户端, 创建一个
                clientConnect();
                return;
            }

            if (draining) {
                //有其他线程正在drainOutbox
                return;
            }

            outBoxMessage = pendingMessages.poll();
            if (Objects.isNull(outBoxMessage)) {
                return;
            }
            draining = true;
        }

        while (true) {
            try {
                TransportClient client;
                synchronized (this) {
                    client = this.client;
                }
                if (Objects.nonNull(client)) {
                    outBoxMessage.sendWith(client);
                }
            } catch (Exception e) {
                handleException(e);
                return;
            }
            synchronized (this) {
                if (isStopped) {
                    return;
                }

                outBoxMessage = pendingMessages.poll();
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
        ExceptionUtils.log(e);
        synchronized (this) {
            if (Objects.isNull(clientConnectFuture)) {
                stop();
                //TODO rpcEnv移除该outbox
            }
        }
    }

    /** 获取与该OutBox绑定的client */
    private void clientConnect() {
        clientConnectFuture = rpcEnv.executors.submit(() -> {
            try {
                TransportClient client = rpcEnv.getClient(address);
                synchronized (OutBox.this) {
                    OutBox.this.client = client;
                    if (isStopped) {
                        closeClient();
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
            drainOutbox();
        });
    }

    /** 关闭client */
    private void closeClient() {
        //为了复用client
        client = null;
    }

    /**
     * outbox stopped
     */
    public void stop() {
        synchronized (this) {
            if (isStopped) {
                return;
            }

            isStopped = true;
            if (Objects.nonNull(clientConnectFuture)) {
                clientConnectFuture.cancel(true);
            }
            closeClient();
        }

        OutBoxMessage outBoxMessage;
        while (Objects.nonNull((outBoxMessage = pendingMessages.poll()))) {
            log.warn("drop message, because of outbox stopped >>>> {}", outBoxMessage);
        }
    }
}
