package org.kin.kinrpc.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.QueryParams;
import com.ecwid.consul.v1.Response;
import com.ecwid.consul.v1.agent.model.NewService;
import com.ecwid.consul.v1.health.HealthServicesRequest;
import com.ecwid.consul.v1.health.model.HealthService;
import org.kin.framework.collection.CopyOnWriteMap;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.ApplicationInstance;
import org.kin.kinrpc.DefaultApplicationInstance;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.config.RegistryConfig;
import org.kin.kinrpc.registry.ApplicationMetadata;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * 以consul为注册中心, 实时监听应用实例状态变化, 并更新可用{@link org.kin.kinrpc.ReferenceInvoker}实例
 * 定时心跳, 上报健康状态
 * 定时轮询发现服务应用变化
 *
 * @author huangjianqin
 * @date 2023/8/12
 */
public class ConsulRegistry extends DiscoveryRegistry {
    private static final Logger log = LoggerFactory.getLogger(ConsulRegistry.class);

    /** consul rest client */
    private final ConsulClient client;
    /** consul heartbeat check interval */
    private final int checkPassInterval;
    /** 服务ttl多久后直接取消注册 */
    private final String deregisterCriticalServiceAfter;
    /** acl token */
    private final String aclToken;
    /** common request query tag */
    private final String qryTag;
    /** consul heartbeat loop */
    private final ConsulHeartbeatLoop heartbeatLoop;
    /** key -> app name, value -> {@link ConsulWatchLoop}实例 */
    private final Map<String, ConsulWatchLoop> watchLoopMap = new CopyOnWriteMap<>();

    public ConsulRegistry(RegistryConfig config) {
        super(config);

        Object[] ipPort = NetUtils.parseIpPort(config.getAddress());
        // TODO: 2023/8/12 TLS
        client = new ConsulClient((String) ipPort[0], (Integer) ipPort[1]);
        checkPassInterval = config.intAttachment(ConsulConstants.CHECK_PASS_INTERVAL_KEY, ConsulConstants.DEFAULT_CHECK_PASS_INTERVAL);
        deregisterCriticalServiceAfter = config.attachment(ConsulConstants.DEREGISTER_CRITICAL_SERVICE_AFTER_KEY, ConsulConstants.DEFAULT_DEREGISTER_CRITICAL_SERVICE_AFTER);
        aclToken = config.attachment(ConsulConstants.ACL_TOKEN_KEY);
        qryTag = config.attachment(ConsulConstants.QUERY_TAG_KEY);
        heartbeatLoop = new ConsulHeartbeatLoop(client, checkPassInterval);
    }

    @Override
    public void init() {
        //do nothing
    }

    @Override
    protected void doRegister(ApplicationMetadata appMetadata) {
        NewService service = new NewService();
        String address = appMetadata.getAddress();
        Object[] ipPort = NetUtils.parseIpPort(address);
        service.setAddress((String) ipPort[0]);
        service.setPort((Integer) ipPort[1]);
        service.setId(generateId(appMetadata));
        service.setName(appMetadata.getAppName());
        service.setCheck(createCheck());
        service.setMeta(getMetadataMap(appMetadata));

        heartbeatLoop.add(appMetadata.getAppName(), service.getId());
        client.agentServiceRegister(service, aclToken);
    }

    /**
     * 创建{@link com.ecwid.consul.v1.agent.model.NewService.Check}实例
     *
     * @return {@link com.ecwid.consul.v1.agent.model.NewService.Check}实例
     */
    private NewService.Check createCheck() {
        NewService.Check check = new NewService.Check();
        //5轮心跳不过就过期
        check.setTtl((checkPassInterval * 5 / 1000) + "s");
        //服务ttl多久后直接取消注册
        check.setDeregisterCriticalServiceAfter(deregisterCriticalServiceAfter);
        return check;
    }

    /**
     * 返回consul app id
     *
     * @param appMetadata app元数据
     * @return consul app id
     */
    private String generateId(ApplicationMetadata appMetadata) {
        return Integer.toHexString(appMetadata.hashCode());
    }

    @Override
    protected void doUnregister(ApplicationMetadata appMetadata) {
        String id = generateId(appMetadata);
        heartbeatLoop.remove(id);
        client.agentServiceDeregister(id, aclToken);
    }

    /**
     * 返回健康的应用服务实例
     *
     * @param appName      app name
     * @param index        consul index
     * @param watchTimeout consul watch timeout
     * @return 健康的应用服务实例信息
     */
    private Response<List<HealthService>> getHealthServices(String appName, long index, int watchTimeout) {
        HealthServicesRequest request = HealthServicesRequest.newBuilder()
                .setTag(qryTag)
                .setQueryParams(new QueryParams(watchTimeout, index))
                .setPassing(true)
                .build();
        return client.getHealthServices(appName, request);
    }

    @Override
    protected void watch(Set<String> appNames) {
        for (String appName : appNames) {
            ConsulWatchLoop watchLoop = watchLoopMap.computeIfAbsent(appName, k -> new ConsulWatchLoop(k, -1));
        }
    }

    @Override
    protected void unwatch(Set<String> appNames) {
        for (String appName : appNames) {
            ConsulWatchLoop watchLoop = watchLoopMap.remove(appName);
            if (Objects.nonNull(watchLoop)) {
                watchLoop.stop();
            }
        }
    }

    @Override
    protected void doDestroy() {
        unwatch(watchLoopMap.keySet());
        heartbeatLoop.stop();
    }

    //-------------------------------------------------------------------------------------------------------------

    /**
     * watch loop
     */
    private class ConsulWatchLoop implements Runnable {
        /** app name */
        private final String appName;
        /** consul index, 类似于version */
        private long consulIndex;
        /** loop stopper or not */
        private boolean stopped;

        ConsulWatchLoop(String appName, long consulIndex) {
            this.appName = appName;
            this.consulIndex = consulIndex;

            //execute watch
            RegistryContext.SCHEDULER.execute(this);
        }

        @Override
        public void run() {
            while (!stopped) {
                //不停watch
                try {
                    onWatch();
                } catch (Exception e) {
                    log.error("%s handle app instance changed fail", e);
                }
            }
        }

        /**
         * watch
         */
        private void onWatch() {
            Response<List<HealthService>> response = getHealthServices(appName, consulIndex, Integer.MAX_VALUE);
            Long currentIndex = response.getConsulIndex();
            if (currentIndex != null && currentIndex > consulIndex) {
                consulIndex = currentIndex;
                List<HealthService> services = response.getValue();
                List<ApplicationInstance> appInstances = new ArrayList<>(services.size());
                for (HealthService healthService : services) {
                    HealthService.Service service = healthService.getService();
                    Map<String, String> meta = service.getMeta();
                    DefaultApplicationInstance appInstance = DefaultApplicationInstance.create()
                            .host(service.getAddress())
                            .port(service.getPort())
                            .scheme(meta.get(PROTOCOL_METADATA_KEY))
                            .revision(meta.get(REVISION_METADATA_KEY))
                            .build();
                    appInstances.add(appInstance);
                }

                onAppInstancesChanged(appName, appInstances);
            }
        }

        /**
         * stop watch loop
         */
        void stop() {
            this.stopped = true;
        }
    }
}
