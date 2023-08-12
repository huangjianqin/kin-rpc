package org.kin.kinrpc.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.kinrpc.RegistryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * 定时向consul发送heartbeat
 *
 * @author huangjianqin
 * @date 2023/8/12
 */
public class ConsulHeartbeatLoop {
    private static final Logger log = LoggerFactory.getLogger(ConsulHeartbeatLoop.class);

    /** key -> app id, value -> heartbeat loop future */
    private final Map<String, ScheduledFuture<?>> serviceHeartbeats = new CopyOnWriteMap<>();
    /** consul client */
    private final ConsulClient client;
    /** 检查间隔 */
    private final long checkInterval;

    public ConsulHeartbeatLoop(ConsulClient client, long checkInterval) {
        this.client = client;
        this.checkInterval = checkInterval;
    }

    /**
     * add app heartbeat loop
     *
     * @param id app id
     */
    public void add(String appName, String id) {
        ScheduledFuture<?> task = RegistryContext.SCHEDULER.scheduleAtFixedRate(
                new HeartbeatTask(appName, id),
                checkInterval,
                checkInterval,
                TimeUnit.MILLISECONDS);
        ScheduledFuture<?> previousTask = this.serviceHeartbeats.put(id, task);
        if (previousTask != null) {
            previousTask.cancel(true);
        }
    }

    /**
     * remove app heartbeat loop
     *
     * @param id app id
     */
    public void remove(String id) {
        ScheduledFuture<?> task = this.serviceHeartbeats.remove(id);
        if (task != null) {
            task.cancel(true);
        }
    }

    /**
     * stop all loop
     */
    public void stop() {
        for (String id : serviceHeartbeats.keySet()) {
            remove(id);
        }
    }

    //----------------------------------------------------------------------------------------------------------------
    private class HeartbeatTask implements Runnable {
        /** app name */
        private final String appName;
        /** app id */
        private final String id;

        HeartbeatTask(String appName, String id) {
            this.appName = appName;
            if (!id.startsWith("service:")) {
                id = "service:" + id;
            }
            this.id = id;
        }

        @Override
        public void run() {
            client.agentCheckPass(this.id);
            if (log.isDebugEnabled()) {
                log.debug(String.format("sending consul heartbeat for '%s'", this.appName));
            }
        }
    }
}
