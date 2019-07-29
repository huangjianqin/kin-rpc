package org.kin.kinrpc.transport.domain;

import io.netty.channel.ChannelOption;
import org.kin.kinrpc.transport.Bytes2ProtocolTransfer;
import org.kin.kinrpc.transport.ChannelExceptionHandler;
import org.kin.kinrpc.transport.DefaultNettySessionBuilder;
import org.kin.kinrpc.transport.DefaultProtocolTransfer;
import org.kin.kinrpc.transport.listener.ChannelActiveListener;
import org.kin.kinrpc.transport.listener.ChannelIdleListener;
import org.kin.kinrpc.transport.listener.ChannelInactiveListener;

import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2019/7/29
 */
public class NettyTransportOption extends TransportOption<NettyTransportOption> {
    protected Map<ChannelOption, Object> channelOptions = new HashMap<>();
    protected Bytes2ProtocolTransfer protocolTransfer;
    protected ChannelActiveListener channelActiveListener;
    protected ChannelInactiveListener channelInactiveListener;
    protected ChannelExceptionHandler channelExceptionHandler;
    protected ChannelIdleListener channelIdleListener;

    public static NettyTransportOption create() {
        return new NettyTransportOption();
    }

    public NettyTransportOption() {
        super.sessionBuilder = DefaultNettySessionBuilder.instance();
        protocolTransfer = DefaultProtocolTransfer.instance();
    }

    public NettyTransportOption channelOptions(Map<ChannelOption, Object> channelOptions) {
        this.channelOptions.putAll(channelOptions);
        return this;
    }

    public <E> NettyTransportOption channelOption(ChannelOption<E> channelOption, E value) {
        this.channelOptions.put(channelOption, value);
        return this;
    }

    public NettyTransportOption protocolTransfer(Bytes2ProtocolTransfer transfer) {
        this.protocolTransfer = transfer;
        return this;
    }

    public NettyTransportOption channelActiveListener(ChannelActiveListener channelActiveListener) {
        this.channelActiveListener = channelActiveListener;
        return this;
    }

    public NettyTransportOption channelInactiveListener(ChannelInactiveListener channelInactiveListener) {
        this.channelInactiveListener = channelInactiveListener;
        return this;
    }

    public NettyTransportOption channelExceptionHandler(ChannelExceptionHandler channelExceptionHandler) {
        this.channelExceptionHandler = channelExceptionHandler;
        return this;
    }

    public NettyTransportOption channelIdleListener(ChannelIdleListener channelIdleListener) {
        this.channelIdleListener = channelIdleListener;
        return this;
    }

    //getter
    public Map<ChannelOption, Object> getChannelOptions() {
        return channelOptions;
    }

    public Bytes2ProtocolTransfer getProtocolTransfer() {
        return protocolTransfer;
    }

    public ChannelActiveListener getChannelActiveListener() {
        return channelActiveListener;
    }

    public ChannelInactiveListener getChannelInactiveListener() {
        return channelInactiveListener;
    }

    public ChannelExceptionHandler getChannelExceptionHandler() {
        return channelExceptionHandler;
    }

    public ChannelIdleListener getChannelIdleListener() {
        return channelIdleListener;
    }
}
