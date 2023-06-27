package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RequestCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * client抽象
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface RemotingClient {
    /**
     * remote connect
     */
    void connect();

    /**
     * client是否存活
     *
     * @return true表示client存活
     */
    boolean isAvailable();

    /**
     * shutdown the client
     */
    void shutdown();

    /**
     * request and get response future
     *
     * @param command request command
     */
    <T> CompletableFuture<T> requestResponse(RequestCommand command);

    /**
     * request and block get response
     *
     * @param command request command
     */
    @SuppressWarnings("unchecked")
    default <T> T bRequestResponse(RequestCommand command) {
        try {
            return (T) requestResponse(command).get();
        } catch (Exception e) {
            throw new RemotingException("request fail", e);
        }
    }

    /**
     * request and get response in {@code timeout} ms or throw exception if time out
     *
     * @param command request command
     */
    @SuppressWarnings("unchecked")
    default <T> T bRequestResponse(RequestCommand command, long timeout) {
        try {
            return (T) requestResponse(command).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            throw new RemotingException("request response fail", e);
        }
    }

    /**
     * just request and ignore response
     *
     * @param command request command
     */
    void fireAndForget(RequestCommand command);
}