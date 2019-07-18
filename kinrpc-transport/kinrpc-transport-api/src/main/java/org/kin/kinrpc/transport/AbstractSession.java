package org.kin.kinrpc.transport;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import org.kin.kinrpc.transport.domain.Response;
import org.kin.kinrpc.transport.protocol.AbstractProtocol;
import org.kin.kinrpc.transport.utils.ChannelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by huangjianqin on 2019/5/30.
 */
public abstract class AbstractSession {
    private static final Logger log = LoggerFactory.getLogger(AbstractSession.class);

    private volatile Channel channel;
    private boolean isFlush;


    private AtomicInteger respSNCounter = new AtomicInteger(1);
    private volatile String ip;
    private volatile long ipHashCode;
    private volatile boolean isClosed;
    private ChannelCloseCause channelCloseCause;
    private AtomicBoolean flushChannelScheduleTag = new AtomicBoolean(false);

    public AbstractSession(Channel channel, boolean isFlush) {
        this.channel = channel;
        this.ip = ChannelUtils.getIP(channel);
        this.ipHashCode = ChannelUtils.ipHashCode(ip);
        this.isFlush = isFlush;
    }

    public Channel changeChannel(Channel channel) {
        if (!isClosed) {
            Channel old = this.channel;
            this.channel = channel;
            this.ip = ChannelUtils.getIP(channel);
            this.ipHashCode = ChannelUtils.ipHashCode(ip);
            return old;
        }

        return null;
    }

    public void sendProtocol(AbstractProtocol protocol) {
        if (protocol != null) {
            Response response = protocol.write();
            if (response != null) {
                write(response);
            }
        }
    }

    public void write(Response response) {
        if (isActive()) {
            if (response != null) {
                if (isFlush) {
                    channel.writeAndFlush(response);
                } else {
                    channel.write(response);
                    if (flushChannelScheduleTag.compareAndSet(false, true)) {
                        scheduleFlush();
                    }
                }
            }
        }
    }

    private void scheduleFlush() {
        channel.eventLoop().schedule(() -> {
            if (flushChannelScheduleTag.compareAndSet(true, false)) {
                channel.flush();
            }
        }, 50, TimeUnit.MILLISECONDS);
    }

    public void writeAndClose(Response response, ChannelCloseCause cause, String ip) {
        if (response != null) {
            ChannelFuture writeFuture = channel.writeAndFlush(response);
            writeFuture.addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture channelFuture) throws Exception {
                    close(cause, ip);
                }
            });
            channel.eventLoop().schedule(() -> {
                if (!writeFuture.isDone()) {
                    close(cause, ip);
                }
            }, 300, TimeUnit.MILLISECONDS);
        }
    }

    public ChannelFuture close(ChannelCloseCause cause, String ip) {
        this.isClosed = true;
        this.channelCloseCause = cause;
        if (channel.isOpen()) {
            log.info("close session('{}') due to Cause: {}", ip, cause);
            return channel.close();
        } else {
            log.info("close closedSession('{}') due to Cause: {}", ip, cause);
            return null;
        }
    }

    public boolean isActive() {
        return channel != null && channel.isActive();
    }

    public int getRespSN() {
        return respSNCounter.getAndIncrement();
    }

    //getter
    public Channel getChannel() {
        return channel;
    }

    public String getIp() {
        return ip;
    }

    public long getIpHashCode() {
        return ipHashCode;
    }

    public boolean isClosed() {
        return isClosed;
    }

    public ChannelCloseCause getChannelCloseCause() {
        return channelCloseCause;
    }

    @Override
    public String toString() {
        return "Session{" +
                "channel=" + channel +
                '}';
    }
}
