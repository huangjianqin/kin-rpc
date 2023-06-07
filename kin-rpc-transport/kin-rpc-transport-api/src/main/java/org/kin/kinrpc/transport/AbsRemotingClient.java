package org.kin.kinrpc.transport;

import org.jctools.maps.NonBlockingHashMap;
import org.kin.kinrpc.transport.cmd.RemotingCodec;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * @author huangjianqin
 * @date 2023/6/7
 */
public abstract class AbsRemotingClient implements RemotingClient {
    /** remoting codec */
    protected final RemotingCodec codec = new RemotingCodec();
    /** remoting processor */
    protected final RemotingProcessor remotingProcessor = new RemotingProcessor(codec);
    /** key -> request id, value -> request future */
    protected final Map<Long, CompletableFuture<Object>> requestFutureMap = new NonBlockingHashMap<>();

    /**
     * 根据{@code requestId}创建request future
     * @return  request future
     */
    protected CompletableFuture<Object> createRequestFuture(long requestId) {
        CompletableFuture<Object> requestFuture = new CompletableFuture<>();
        CompletableFuture<Object> current = requestFutureMap.computeIfAbsent(requestId, i -> requestFuture);
        if (current != requestFuture) {
            throw new RemotingException(String.format("request id(%d) duplicate!!!", requestId));
        }

        return current;
    }

    /**
     * 根据{@code requestId}获取request future
     * @param requestId request id
     * @return  request future
     */
    @Nullable
    protected CompletableFuture<Object> removeRequestFuture(long requestId){
        return requestFutureMap.remove(requestId);
    }
}
