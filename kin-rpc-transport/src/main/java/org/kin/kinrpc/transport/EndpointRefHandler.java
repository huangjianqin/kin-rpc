package org.kin.kinrpc.transport;

import io.netty.channel.Channel;
import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.ExecutionContext;
import org.kin.kinrpc.transport.protocol.RpcResponseProtocol;
import org.kin.transport.netty.core.Client;
import org.kin.transport.netty.core.ClientTransportOption;
import org.kin.transport.netty.core.TransportHandler;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-06-08
 */
public abstract class EndpointRefHandler extends TransportHandler {
    private static final Logger log = LoggerFactory.getLogger(EndpointRefHandler.class);
    /** 客户端重连线程池 */
    public static ExecutionContext RECONNECT_EXECUTORS =
            ExecutionContext.fix(
                    2, "endpointRef-reconnect",
                    1, "endpointRef-reconnect-scheduler");

    static {
        JvmCloseCleaner.DEFAULT().add(() -> RECONNECT_EXECUTORS.shutdownNow());
    }

    /** 客户端引用 */
    protected volatile Client client;
    /** 是否已关闭 */
    protected volatile boolean isStopped;

    /**
     * 连接服务器
     */
    public final void connect(ClientTransportOption transportOption, InetSocketAddress address) {
        if (isStopped) {
            return;
        }
        if (isActive()) {
            return;
        }
        if (client != null) {
            client.close();
            client = null;
        }
        if (client == null) {
            try {
                client = transportOption.tcp(address);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            if (!isActive()) {
                //n秒后重连
                RECONNECT_EXECUTORS.schedule(() -> connect(transportOption, address), 5, TimeUnit.SECONDS);
            }
        }
    }

    /**
     * 关闭客户端
     */
    public final void close() {
        if (Objects.nonNull(client)) {
            isStopped = true;
            client.close();
        }
    }

    /**
     * 客户端是否有效
     */
    public final boolean isActive() {
        return !isStopped && client != null && client.isActive();
    }

    /**
     * 处理返回协议
     */
    @Override
    public final void handleProtocol(Channel channel, AbstractProtocol protocol) {
        if (!isActive()) {
            return;
        }
        if (Objects.isNull(protocol)) {
            return;
        }
        if (protocol instanceof RpcResponseProtocol) {
            RpcResponseProtocol responseProtocol = (RpcResponseProtocol) protocol;
            handleRpcResponseProtocol(responseProtocol);
        } else {
            //未知协议, 异常直接关闭channel
            channel.close();
            log.error("server({}) send unknown protocol >>>> {}", channel.remoteAddress(), protocol);
        }
    }

    /**
     * 客户端断开处理
     *
     * @param channel
     */
    @Override
    public final void channelInactive(Channel channel) {
        RECONNECT_EXECUTORS.execute(this::reconnect);
    }

    /**
     * 处理返回协议
     */
    protected abstract void handleRpcResponseProtocol(RpcResponseProtocol responseProtocol);

    /**
     * 重连逻辑
     */
    protected abstract void reconnect();
}
