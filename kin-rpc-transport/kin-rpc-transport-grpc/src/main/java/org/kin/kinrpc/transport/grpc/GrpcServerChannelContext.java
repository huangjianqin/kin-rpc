package org.kin.kinrpc.transport.grpc;

import io.grpc.Attributes;
import io.grpc.Grpc;
import io.grpc.stub.StreamObserver;
import io.netty.buffer.ByteBuf;
import org.kin.kinrpc.transport.ChannelContext;
import org.kin.kinrpc.transport.TransportException;
import org.kin.kinrpc.transport.TransportOperationListener;

import javax.annotation.Nullable;
import java.net.SocketAddress;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/8
 */
public class GrpcServerChannelContext implements ChannelContext {
    private final GrpcServerCallContext callContext = GrpcServerCallContext.current();
    private final StreamObserver<ByteBuf> observer;

    public GrpcServerChannelContext(StreamObserver<ByteBuf> observer) {
        this.observer = observer;
    }

    @Override
    public void writeAndFlush(Object msg, @Nullable TransportOperationListener listener) {
        if (!(msg instanceof ByteBuf)) {
            throw new TransportException(String.format("illegal outbound message type '%s'", msg.getClass()));
        }

        try {
            observer.onNext((ByteBuf) msg);
            observer.onCompleted();
            listener.onComplete();
        } catch (Exception e) {
            listener.onFailure(e);
        }
    }

    @Override
    public SocketAddress address() {
        if (Objects.nonNull(callContext)) {
            return callContext.attributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        }
        return null;
    }
}
