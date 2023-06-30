package org.kin.kinrpc.transport;

import org.kin.framework.collection.ConcurrentHashSet;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadPoolUtils;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.transport.cmd.CodecException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * 管理所有{@link RemotingClient}实例, 实现定时健康检查, 及重连
 *
 * @author huangjianqin
 * @date 2023/6/21
 */
public final class RemotingClientMonitor {
    private static final Logger log = LoggerFactory.getLogger(RemotingClientMonitor.class);

    /** remoting client健康检查scheduler */
    private static final ScheduledThreadPoolExecutor SCHEDULER = ThreadPoolUtils.newScheduledThreadPool("remoting-client-monitor", true,
            SysUtils.CPU_NUM, new SimpleThreadFactory("remoting-client-monitor", true),
            new ThreadPoolExecutor.CallerRunsPolicy());
    /** unhealth exception */
    private static final Predicate<Throwable> UNHEALTH_EXCEPTION = t -> !(t instanceof RemotingException) && !(t instanceof CodecException);
    /** 心跳超时时间 */
    private static final int HEARTBEAT_TIMEOUT;
    /** 重连超时时间 */
    private static final int RECONNECT_TIMEOUT;
    /** 重连间隔时间 */
    private static final int RECONNECT_INTERVAL;

    /** 所有remoting client实例 */
    private static final Set<RemotingClientObserver> CLIENT_OBSERVERS = new ConcurrentHashSet<>();
    /** unhealthy remoting client实例 */
    private static final Set<RemotingClientObserver> UNHEALTH_CLIENT_OBSERVERS = new ConcurrentHashSet<>();

    static {
        //心跳超时时间
        HEARTBEAT_TIMEOUT = SysUtils.getIntSysProperty("kinrpc.transport.heartbeat.timeout", 3000);
        //心跳间隔
        int heartbeatRate = SysUtils.getIntSysProperty("kinrpc.transport.heartbeat.rate", 5000);
        SCHEDULER.scheduleAtFixedRate(RemotingClientMonitor::healthCheck, heartbeatRate, heartbeatRate, TimeUnit.MILLISECONDS);
        //重连超时时间
        RECONNECT_TIMEOUT = SysUtils.getIntSysProperty("kinrpc.transport.reconnect.timeout", 3000);
        //重连间隔时间
        RECONNECT_INTERVAL = SysUtils.getIntSysProperty("kinrpc.transport.reconnect.interval", 10_000);
    }

    private RemotingClientMonitor() {
    }

    /**
     * 新增remoting client
     *
     * @param clientObserver remoting client observer
     */
    public static void addClient(RemotingClientObserver clientObserver) {
        UNHEALTH_CLIENT_OBSERVERS.remove(clientObserver);
        CLIENT_OBSERVERS.add(clientObserver);
        clientObserver.toHealth();
    }

    /**
     * 移除remoting client
     *
     * @param clientObserver remoting client observer
     */
    public static void removeClient(RemotingClientObserver clientObserver) {
        CLIENT_OBSERVERS.remove(clientObserver);
        UNHEALTH_CLIENT_OBSERVERS.remove(clientObserver);
    }

    /**
     * 心跳检查
     */
    private static void healthCheck() {
        List<CompletableFuture<Void>> heartbeatFutures = new ArrayList<>();
        for (RemotingClientObserver observer : CLIENT_OBSERVERS) {
            CompletableFuture<Void> heartbeatFuture = observer.heartbeat();
            heartbeatFuture.whenCompleteAsync((r, t) -> {
                //心跳失败
                if (Objects.nonNull(t)) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    onHeartbeatFail(observer, t);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("{} receive heartbeat ack", observer.getName());
                    }
                }
            }, SCHEDULER);
            heartbeatFutures.add(heartbeatFuture);
        }

        if (CollectionUtils.isNonEmpty(heartbeatFutures)) {
            //定时调度心跳超时
            SCHEDULER.schedule(() -> {
                for (CompletableFuture<Void> heartbeatFuture : heartbeatFutures) {
                    if (heartbeatFuture.isDone()) {
                        continue;
                    }
                    heartbeatFuture.completeExceptionally(HeartbeatTimeoutException.INSTANCE);
                }
            }, HEARTBEAT_TIMEOUT, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * request异常也算上unhealth
     *
     * @param observer remoting client observer
     * @param t        request exception
     */
    public static void onRequestFail(RemotingClientObserver observer, Throwable t) {
        SCHEDULER.execute(() -> onTransportOperationFail(observer, t, "send request"));
    }

    /**
     * heartbeat异常也算上unhealth
     *
     * @param observer remoting client observer
     * @param t        request exception
     */
    public static void onHeartbeatFail(RemotingClientObserver observer, Throwable t) {
        onTransportOperationFail(observer, t, "send heartbeat");
    }

    /**
     * connect异常也算上unhealth
     *
     * @param observer remoting client observer
     * @param t        request exception
     */
    public static void onConnectFail(RemotingClientObserver observer, Throwable t) {
        SCHEDULER.execute(() -> onTransportOperationFail(observer, t, "connect to remote"));
    }

    /**
     * transport层异常也算上unhealth
     *
     * @param observer remoting client observer
     * @param t        request exception
     */
    private static void onTransportOperationFail(RemotingClientObserver observer, Throwable t, String opr) {
        if (!UNHEALTH_EXCEPTION.test(t)) {
            return;
        }

        log.error("{} {} fail, try to reconnect to remote", observer.getName(), opr, t);
        reconnect(observer);
    }

    /**
     * remote挂了或者强制关闭client时, 尝试重连
     *
     * @param observer remoting client observer
     */
    public static void onClientTerminated(RemotingClientObserver observer) {
        SCHEDULER.execute(() -> {
            log.error("remote down or remote force close {} connection, try to reconnect to remote", observer.getName());
            reconnect(observer);
        });
    }

    /**
     * remoting client reconnect
     *
     * @param observer remoting client
     */
    private static void reconnect(RemotingClientObserver observer) {
        CLIENT_OBSERVERS.remove(observer);
        UNHEALTH_CLIENT_OBSERVERS.add(observer);
        observer.toUnhealth();
        reconnect(observer, 0);
    }

    /**
     * remoting client reconnect
     *
     * @param observer remoting client
     * @param times    reconnected times
     */
    private static void reconnect(RemotingClientObserver observer, int times) {
        if (!UNHEALTH_CLIENT_OBSERVERS.contains(observer)) {
            return;
        }

        CompletableFuture<Void> reconnectFuture = observer.reconnect();
        if (Objects.isNull(reconnectFuture)) {
            return;
        }

        reconnectFuture.whenCompleteAsync((r, t) -> {
            onReconnectComplete(observer, t, times + 1);
        }, SCHEDULER);
        SCHEDULER.schedule(() -> {
            if (reconnectFuture.isDone()) {
                return;
            }
            reconnectFuture.completeExceptionally(ReconnectTimeoutException.INSTANCE);
        }, RECONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    /**
     * remoting client reconnect complete
     *
     * @param observer remoting client
     * @param t        reconnect exception
     * @param times    reconnected times
     */
    private static void onReconnectComplete(RemotingClientObserver observer, Throwable t, int times) {
        if (Objects.isNull(t)) {
            //reconnect success
            addClient(observer);
        } else {
            //reconnect fail
            log.error("{} reconnect fail {} times, retry to reconnect", observer.getName(), times, t);
            //线性递增
            int delay = Math.min((times) * 1000, RECONNECT_INTERVAL);
            SCHEDULER.schedule(() -> reconnect(observer, times), delay, TimeUnit.MILLISECONDS);
        }
    }
}
