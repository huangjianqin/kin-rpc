package org.kin.kinrpc.rpc;

import com.google.common.net.HostAndPort;
import io.netty.channel.Channel;
import io.netty.channel.ChannelOption;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.TimeUtils;
import org.kin.kinrpc.rpc.future.RPCFuture;
import org.kin.kinrpc.rpc.serializer.Serializer;
import org.kin.kinrpc.rpc.transport.common.RPCConstants;
import org.kin.kinrpc.rpc.transport.domain.RPCRequest;
import org.kin.kinrpc.rpc.transport.domain.RPCResponse;
import org.kin.kinrpc.rpc.transport.protocol.RPCHeartbeat;
import org.kin.kinrpc.rpc.transport.protocol.RPCRequestProtocol;
import org.kin.kinrpc.rpc.transport.protocol.RPCResponseProtocol;
import org.kin.transport.netty.core.Client;
import org.kin.transport.netty.core.ClientTransportOption;
import org.kin.transport.netty.core.TransportHandler;
import org.kin.transport.netty.core.TransportOption;
import org.kin.transport.netty.core.protocol.AbstractProtocol;
import org.kin.transport.netty.core.protocol.ProtocolFactory;
import org.kin.transport.netty.core.statistic.InOutBoundStatisicService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/14.
 */
public class RPCReference {
    private static final Logger log = LoggerFactory.getLogger(RPCReference.class);
    private volatile boolean isStopped;

    private Map<String, RPCFuture> pendingRPCFutureMap = new ConcurrentHashMap<>();

    private String serviceName;
    private InetSocketAddress address;
    private Serializer serializer;
    private ClientTransportOption clientTransportOption;
    private ReferenceHandler referenceHandler;
    private HeartBeatCallBack heartBeatCallBack;

    public RPCReference(String serviceName, InetSocketAddress address, Serializer serializer, int connectTimeout, boolean compression, HeartBeatCallBack heartBeatCallBack) {
        this.serviceName = serviceName;
        this.address = address;
        this.serializer = serializer;
        this.referenceHandler = new ReferenceHandler();
        this.clientTransportOption =
                TransportOption.client()
                        .channelOption(ChannelOption.TCP_NODELAY, true)
                        .channelOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                        .transportHandler(this.referenceHandler);
        if (compression) {
            this.clientTransportOption.compress();
        }
        this.heartBeatCallBack = heartBeatCallBack;
    }

    /**
     * channel线程
     */
    public void handleResponse(RPCResponse rpcResponse) {
        if (isStopped) {
            return;
        }
        rpcResponse.setHandleTime(System.currentTimeMillis());

        log.debug("receive a response >>> " + System.lineSeparator() + rpcResponse);

        String requestId = rpcResponse.getRequestId();
        RPCFuture pendRPCFuture = pendingRPCFutureMap.get(requestId);
        if (pendRPCFuture != null) {
            pendRPCFuture.done(rpcResponse);
        }
    }

    /**
     * 其他线程
     */
    public Future<RPCResponse> request(RPCRequest request) {
        RPCFuture future = new RPCFuture(request, this);
        if (!isActive()) {
            future.done(RPCResponse.respWithError(request, "client channel closed"));
            return future;
        }
        log.debug("send a request>>>" + System.lineSeparator() + request);

        try {
            referenceHandler.request(request);
            pendingRPCFutureMap.put(request.getRequestId() + "", future);
        } catch (Exception e) {
            pendingRPCFutureMap.remove(request.getRequestId() + "");
            future.done(RPCResponse.respWithError(request,
                    "client channel closed, due to ".concat(System.lineSeparator()).concat(ExceptionUtils.getExceptionDesc(e))));
        }

        return future;
    }

    public void clean() {
        for (RPCFuture rpcFuture : pendingRPCFutureMap.values()) {
            RPCRequest rpcRequest = rpcFuture.getRequest();
            RPCResponse rpcResponse = RPCResponse.respWithRetry(rpcRequest, "channel inactive");
            rpcFuture.done(rpcResponse);
        }
        this.pendingRPCFutureMap.clear();
    }

    public HostAndPort getAddress() {
        return HostAndPort.fromString(this.address.getHostName() + ":" + this.address.getPort());
    }

    public boolean isActive() {
        if (isStopped) {
            return false;
        }
        return referenceHandler.isActive();
    }

    public void start() {
        if (isStopped) {
            return;
        }
        referenceHandler.connect(clientTransportOption);
    }

    public void shutdown() {
        if (isStopped) {
            return;
        }
        isStopped = true;
        referenceHandler.close();
        clean();
    }

    /**
     * 已在pendingRPCFutureMap锁内执行
     */
    public void removeInvalid(RPCRequest rpcRequest) {
        this.pendingRPCFutureMap.remove(rpcRequest.getRequestId() + "");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RPCReference that = (RPCReference) o;
        return serviceName.equals(that.serviceName) &&
                address.equals(that.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceName, address);
    }

    //------------------------------------------------------------------------------------------------------------------
    public interface HeartBeatCallBack {
        void heartBeatFail(RPCReference rpcReference);
    }

    private class ReferenceHandler extends TransportHandler {
        /** 心跳间隔(秒) */
        private final int HEARTBEAT_INTERNAL = 10;
        /** 最大心跳失败次数 */
        private final int MAX_HEARTBEAT_FAIL = 3;

        private volatile Client client;
        private volatile Future heartbeatFuture;
        private ClientTransportOption transportOption;

        private int lastHeartBeatTime;
        private int heartBeatFailTimes;

        public void connect(ClientTransportOption transportOption) {
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
                    RPCThreadPool.THREADS.schedule(() -> connect(transportOption), 5, TimeUnit.SECONDS);
                }
            }

            if (heartbeatFuture == null) {
                heartbeatFuture = RPCThreadPool.THREADS.scheduleAtFixedRate(() -> {
                    if (client != null && checkHeartbeat()) {
                        try {
                            RPCHeartbeat heartbeat = ProtocolFactory.createProtocol(RPCConstants.RPC_HEARTBEAT_PROTOCOL_ID, client.getLocalAddress(), "");
                            client.request(heartbeat);
                        } catch (Exception e) {
                            //屏蔽异常
                        }
                    }
                }, HEARTBEAT_INTERNAL, HEARTBEAT_INTERNAL, TimeUnit.SECONDS);
            }
        }

        public void close() {
            if (Objects.nonNull(client)) {
                client.close();
            }
            if (Objects.nonNull(heartbeatFuture)) {
                heartbeatFuture.cancel(true);
            }
        }

        public boolean isActive() {
            return !isStopped && client != null && client.isActive();
        }

        public void request(RPCRequest request) {
            if (isActive()) {
                try {
                    request.setCreateTime(System.currentTimeMillis());
                    byte[] data = serializer.serialize(request);

                    RPCRequestProtocol protocol = ProtocolFactory.createProtocol(RPCConstants.RPC_REQUEST_PROTOCOL_ID, data);
                    client.request(protocol);

                    InOutBoundStatisicService.instance().statisticReq(
                            request.getServiceName() + "-" + request.getMethod(), data.length
                    );
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }

        @Override
        public void handleProtocol(Channel channel, AbstractProtocol protocol) {
            if (!isActive()) {
                return;
            }
            if (Objects.isNull(protocol)) {
                return;
            }
            if (protocol instanceof RPCResponseProtocol) {
                RPCResponseProtocol responseProtocol = (RPCResponseProtocol) protocol;
                try {
                    RPCResponse rpcResponse;
                    try {
                        rpcResponse = serializer.deserialize(responseProtocol.getRespContent(), RPCResponse.class);
                        rpcResponse.setEventTime(System.currentTimeMillis());
                    } catch (IOException | ClassNotFoundException e) {
                        log.error(e.getMessage(), e);
                        return;
                    }

                    InOutBoundStatisicService.instance().statisticResp(
                            rpcResponse.getServiceName() + "-" + rpcResponse.getMethod(), responseProtocol.getRespContent().length
                    );

                    handleResponse(rpcResponse);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            } else if (protocol instanceof RPCHeartbeat) {
                RPCHeartbeat heartbeat = (RPCHeartbeat) protocol;
                lastHeartBeatTime = TimeUtils.timestamp();
                log.info("reference({}) receive heartbeat ip:{}, content:{}", serviceName, heartbeat.getIp(), heartbeat.getContent());
            } else {
                log.error("unknown protocol >>>> {}", protocol);
            }
        }

        @Override
        public void channelInactive(Channel channel) {
            RPCThreadPool.THREADS.execute(() -> {
                RPCReference.this.clean();
                if (!isStopped) {
                    connect(clientTransportOption);
                }
            });
        }

        /**
         * 检查心跳是否失败, 如果失败, 触发callback
         *
         * @return 心跳是否失败
         */
        private boolean checkHeartbeat() {
            if (heartBeatFailTimes < MAX_HEARTBEAT_FAIL) {
                int now = TimeUtils.timestamp();
                if (now - lastHeartBeatTime > HEARTBEAT_INTERNAL) {
                    //上次心跳还未返回
                    heartBeatFailTimes++;
                    if (heartBeatFailTimes >= MAX_HEARTBEAT_FAIL) {
                        heartBeatCallBack.heartBeatFail(RPCReference.this);
                        return false;
                    }
                }
                return true;
            }

            return false;
        }
    }
}
