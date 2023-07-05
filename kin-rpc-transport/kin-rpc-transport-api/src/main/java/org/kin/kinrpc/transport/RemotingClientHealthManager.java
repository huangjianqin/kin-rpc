package org.kin.kinrpc.transport;

import org.kin.framework.collection.ConcurrentHashSet;
import org.kin.framework.collection.Tuple;
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
public final class RemotingClientHealthManager {
    private static final Logger log = LoggerFactory.getLogger(RemotingClientHealthManager.class);

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
    private static final Set<RemotingClientHelper> CLIENT_HELPERS = new ConcurrentHashSet<>();
    /** unhealthy remoting client实例 */
    private static final Set<RemotingClientHelper> UNHEALTH_CLIENT_HELPERS = new ConcurrentHashSet<>();

    static {
        //心跳超时时间
        HEARTBEAT_TIMEOUT = SysUtils.getIntSysProperty("kinrpc.transport.heartbeat.timeout", 3000);
        //心跳间隔
        int heartbeatRate = SysUtils.getIntSysProperty("kinrpc.transport.heartbeat.rate", 5000);
        SCHEDULER.scheduleAtFixedRate(RemotingClientHealthManager::healthCheck, heartbeatRate, heartbeatRate, TimeUnit.MILLISECONDS);
        //重连超时时间
        RECONNECT_TIMEOUT = SysUtils.getIntSysProperty("kinrpc.transport.reconnect.timeout", 3000);
        //重连间隔时间
        RECONNECT_INTERVAL = SysUtils.getIntSysProperty("kinrpc.transport.reconnect.interval", 10_000);
    }

    private RemotingClientHealthManager() {
    }

    /**
     * 新增remoting client
     *
     * @param clientHelper remoting client helper
     */
    public static void addClient(RemotingClientHelper clientHelper) {
        UNHEALTH_CLIENT_HELPERS.remove(clientHelper);
        CLIENT_HELPERS.add(clientHelper);
        clientHelper.toHealth();
    }

    /**
     * 移除remoting client
     *
     * @param clientHelper remoting client helper
     */
    public static void removeClient(RemotingClientHelper clientHelper) {
        CLIENT_HELPERS.remove(clientHelper);
        UNHEALTH_CLIENT_HELPERS.remove(clientHelper);
    }

    /**
     * 心跳检查
     */
    private static void healthCheck() {
        List<Tuple<RemotingClientHelper, CompletableFuture<Void>>> heartbeatDataList = new ArrayList<>();
        for (RemotingClientHelper helper : CLIENT_HELPERS) {
            CompletableFuture<Void> heartbeatFuture = helper.heartbeat();
            heartbeatFuture.whenCompleteAsync((r, t) -> {
                //心跳失败
                if (Objects.nonNull(t)) {
                    if (t instanceof CompletionException) {
                        t = t.getCause();
                    }
                    onHeartbeatFail(helper, t);
                } else {
                    if (log.isDebugEnabled()) {
                        log.debug("{} receive heartbeat ack", helper.getName());
                    }
                }
            }, SCHEDULER);
            heartbeatDataList.add(new Tuple<>(helper, heartbeatFuture));
        }

        if (CollectionUtils.isNonEmpty(heartbeatDataList)) {
            //定时调度心跳超时
            SCHEDULER.schedule(() -> {
                for (Tuple<RemotingClientHelper, CompletableFuture<Void>> tuple : heartbeatDataList) {
                    RemotingClientHelper helper = tuple.first();
                    if (helper.isTerminated()) {
                        continue;
                    }

                    CompletableFuture<Void> heartbeatFuture = tuple.second();
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
     * @param helper remoting client helper
     * @param t      request exception
     */
    public static void onRequestFail(RemotingClientHelper helper, Throwable t) {
        SCHEDULER.execute(() -> onTransportOperationFail(helper, t, "send request"));
    }

    /**
     * heartbeat异常也算上unhealth
     *
     * @param helper remoting client helper
     * @param t        request exception
     */
    public static void onHeartbeatFail(RemotingClientHelper helper, Throwable t) {
        onTransportOperationFail(helper, t, "send heartbeat");
    }

    /**
     * connect异常也算上unhealth
     *
     * @param helper remoting client helper
     * @param t        request exception
     */
    public static void onConnectFail(RemotingClientHelper helper, Throwable t) {
        SCHEDULER.execute(() -> onTransportOperationFail(helper, t, "connect to remote"));
    }

    /**
     * transport层异常也算上unhealth
     *
     * @param helper remoting client helper
     * @param t        request exception
     */
    private static void onTransportOperationFail(RemotingClientHelper helper, Throwable t, String opr) {
        if (!UNHEALTH_EXCEPTION.test(t)) {
            return;
        }

        log.error("{} {} fail, try to reconnect to remote", helper.getName(), opr, t);
        reconnect(helper);
    }

    /**
     * remote挂了或者强制关闭client时, 尝试重连
     *
     * @param helper remoting client helper
     */
    public static void onClientTerminated(RemotingClientHelper helper) {
        SCHEDULER.execute(() -> {
            log.error("remote down or remote force close {} connection, try to reconnect to remote", helper.getName());
            reconnect(helper);
        });
    }

    /**
     * remoting client reconnect
     *
     * @param helper remoting client helper
     */
    private static void reconnect(RemotingClientHelper helper) {
        CLIENT_HELPERS.remove(helper);
        UNHEALTH_CLIENT_HELPERS.add(helper);
        helper.toUnhealth();
        reconnect(helper, 0);
    }

    /**
     * remoting client reconnect
     *
     * @param helper remoting client helper
     * @param times    reconnected times
     */
    private static void reconnect(RemotingClientHelper helper, int times) {
        if (!UNHEALTH_CLIENT_HELPERS.contains(helper)) {
            return;
        }

        CompletableFuture<Void> reconnectFuture = helper.reconnect();
        if (Objects.isNull(reconnectFuture)) {
            return;
        }

        reconnectFuture.whenCompleteAsync((r, t) -> {
            onReconnectComplete(helper, t, times + 1);
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
     * @param helper remoting client helper
     * @param t        reconnect exception
     * @param times    reconnected times
     */
    private static void onReconnectComplete(RemotingClientHelper helper, Throwable t, int times) {
        if (Objects.isNull(t)) {
            //reconnect success
            addClient(helper);
        } else {
            //reconnect fail
            log.error("{} reconnect fail {} times, retry to reconnect", helper.getName(), times, t);
            //线性递增
            int delay = Math.min((times) * 1000, RECONNECT_INTERVAL);
            SCHEDULER.schedule(() -> reconnect(helper, times), delay, TimeUnit.MILLISECONDS);
        }
    }
}
