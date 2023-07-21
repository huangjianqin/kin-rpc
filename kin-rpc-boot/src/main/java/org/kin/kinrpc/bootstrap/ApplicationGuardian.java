package org.kin.kinrpc.bootstrap;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.RegistryContext;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.constants.KinRpcSystemProperties;
import org.kin.kinrpc.registry.DiscoveryRegistry;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.metadata.service.MetadataServiceImpl;
import org.kin.kinrpc.service.MetadataService;
import org.kin.kinrpc.utils.ServiceUtils;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author huangjianqin
 * @date 2023/7/21
 */
public final class ApplicationGuardian {
    /** 单例 */
    private static final ApplicationGuardian INSTANCE = new ApplicationGuardian();

    static {
        JvmCloseCleaner.instance().add(() -> instance().destroy());
    }

    /** 初始状态 */
    private static final byte INIT_STATE = 1;
    /** started状态 */
    private static final byte STARTED_STATE = 2;
    /** terminated后状态 */
    private static final byte TERMINATED_STATE = 3;

    public static ApplicationGuardian instance() {
        return INSTANCE;
    }

    /** 状态 */
    private final AtomicInteger state = new AtomicInteger(INIT_STATE);


    /** 已暴露内部服务的地址 */
    private final Set<String> internalServiceExportedAddresses = new LinkedHashSet<>();
    /** 应用实例服务元数据刷新future */
    private ScheduledFuture<?> refreshFuture;
    /** 已刷新应用实例服务元数据计数 */
    private final AtomicInteger instanceRefreshScheduleTimes = new AtomicInteger(0);
    /** 已暴露服务计数 */
    private final AtomicInteger serviceExportedCounter = new AtomicInteger();

    private ApplicationGuardian() {
    }

    /**
     * 暴露内部服务
     *
     * @param serverConfig server配置
     */
    public void exportInternalService(ServerConfig serverConfig) {
        String address = serverConfig.getAddress();
        if (!internalServiceExportedAddresses.add(address)) {
            //已部署内置服务
            return;
        }

        //暴露元数据服务
        ServiceConfig<MetadataService> metadataServiceConfig = ServiceUtils.createInternalService(MetadataService.class, serverConfig, MetadataServiceImpl.instance());
        metadataServiceConfig.export();
    }

    /**
     * 服务exported触发
     */
    public void onServiceExported(ServiceConfig<?> serviceConfig) {
        if (ServiceUtils.isInternalService(serviceConfig.getInterfaceClass())) {
            //内部服务不计入
            return;
        }

        serviceExportedCounter.incrementAndGet();
    }

    /**
     * operation after started
     */
    public void onStarted() {
        if (!state.compareAndSet(INIT_STATE, STARTED_STATE)) {
            return;
        }

        int refreshDelay = SysUtils.getIntSysProperty(KinRpcSystemProperties.METADATA_REFRESH_DELAY, 1000);
        refreshFuture = RegistryContext.SCHEDULER.scheduleWithFixedDelay(this::refreshAppInstanceOrMetadata, 0, refreshDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * 刷新注册中心应用实例
     */
    private void refreshAppInstanceOrMetadata() {
        if (isTerminated()) {
            return;
        }

        if (serviceExportedCounter.get() < 1) {
            return;
        }

        // 默认每5s刷新一次
        if (instanceRefreshScheduleTimes.get() % 5 != 0) {
            return;
        }

        for (DiscoveryRegistry discoveryRegistry : RegistryHelper.getDiscoveryRegistries()) {
            discoveryRegistry.refreshAppInstances();
        }
    }

    /**
     * destroy
     */
    public void destroy() {
        if (!state.compareAndSet(STARTED_STATE, TERMINATED_STATE)) {
            return;
        }

        if (Objects.nonNull(refreshFuture)) {
            refreshFuture.cancel(true);
        }
    }

    //getter
    public boolean isTerminated() {
        return state.get() == TERMINATED_STATE;
    }
}
