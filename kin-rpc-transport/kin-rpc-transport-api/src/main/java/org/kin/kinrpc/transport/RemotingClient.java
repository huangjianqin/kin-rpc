package org.kin.kinrpc.transport;

import org.kin.kinrpc.transport.cmd.RequestCommand;

import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * client抽象
 *
 * @author huangjianqin
 * @date 2023/6/1
 */
public interface RemotingClient {
    /** fake heartbeat返回值 */
    CompletableFuture<Void> FAKE_HEARTBEAT = CompletableFuture.completedFuture(null);

    /**
     * remote connect, block util connect success or fail
     */
    void connect();

    /**
     * client是否存活
     *
     * @return true表示client存活
     */
    boolean isAvailable();

    /**
     * 返回remote address
     *
     * @return remote address
     */
    String remoteAddress();

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
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new RemotingException("request response fail", t);
        }
    }

    /**
     * request and get response in {@code timeout} ms or throw exception if time out
     *
     * @param command request command
     * @param timeout block timeout
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
     * @return complete signal
     */
    CompletableFuture<Void> fireAndForget(RequestCommand command);

    /**
     * just request and block until rpc request send success
     *
     * @param command request command
     */
    default void bFireAndForget(RequestCommand command) {
        try {
            fireAndForget(command).get();
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new RemotingException("request response fail", t);
        }
    }

    /**
     * just request and block until rpc request send success or throw exception if time out
     *
     * @param command request command
     * @param timeout block timeout
     */
    default void bFireAndForget(RequestCommand command, long timeout) {
        try {
            fireAndForget(command).get(timeout, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Throwable t = e;
            if (e instanceof ExecutionException) {
                t = e.getCause();
            }
            throw new RemotingException("request response fail", t);
        }
    }

    /**
     * 添加{@link  RemotingClientStateObserver}实例
     *
     * @param observers client state observer collection
     */
    void addObservers(Collection<RemotingClientStateObserver> observers);

    /**
     * 添加{@link  RemotingClientStateObserver}实例
     *
     * @param observers client state observer collection
     */
    default void addObservers(RemotingClientStateObserver... observers) {
        addObservers(Arrays.asList(observers));
    }
}
