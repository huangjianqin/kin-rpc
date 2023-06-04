package org.kin.kinrpc.transport;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface RemotingClient {

    <T> CompletableFuture<T> requestResponse();

    @SuppressWarnings("unchecked")
    default <T> T bRequestResponse(){
        try {
            return (T) requestResponse().get();
        } catch (Exception e) {
            throw new TransportException("request fail", e);
        }
    }

    @SuppressWarnings("unchecked")
    default <T> T bRequestResponse(long timeout){
        try {
            return (T) requestResponse().get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new TransportException("request response fail", e);
        }
    }

    void fireAndForget();
}
