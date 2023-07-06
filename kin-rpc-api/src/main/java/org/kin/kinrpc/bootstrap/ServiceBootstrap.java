package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.KinRpcAppContext;
import org.kin.kinrpc.KinRpcRuntimeContext;
import org.kin.kinrpc.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * service bootstrap抽象父类
 *
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(alias = "serviceBootstrap", singleton = false)
public abstract class ServiceBootstrap<T> {
    private static final Logger log = LoggerFactory.getLogger(ServiceBootstrap.class);
    /** 已发布的服务gsv */
    private static final Set<String> EXPORTED_SERVICES = new CopyOnWriteArraySet<>();
    /** 初始状态 */
    private static final byte INIT_STATE = 1;
    /** exported状态 */
    private static final byte EXPORTED_STATE = 2;
    /** unExported后状态 */
    private static final byte TERMINATED_STATE = 3;

    /** 服务配置 */
    protected final ServiceConfig<T> config;
    /** 状态 */
    private final AtomicInteger state = new AtomicInteger(INIT_STATE);

    protected ServiceBootstrap(ServiceConfig<T> config) {
        this.config = config;
    }

    /**
     * 发布服务
     */
    public final void export() {
        if (!state.compareAndSet(INIT_STATE, EXPORTED_STATE)) {
            return;
        }

        long delay = config.getDelay();
        if (delay > 0) {
            log.info("service '{}' will auto export after {} ms", config.getService(), delay);
            KinRpcAppContext.SCHEDULER.schedule(this::export0, delay, TimeUnit.MILLISECONDS);
        } else {
            export0();
        }
    }

    /**
     * 发布服务
     */
    private void export0() {
        String service = config.getService();
        if (EXPORTED_SERVICES.contains(service)) {
            log.warn("service '{}' has been exported before, " +
                    " please check and ensure duplicate export is right operation, " +
                    "ignore this if you did that on purpose!", service);
        }

        doExport();
        KinRpcRuntimeContext.cacheService(this);

        log.info("service '{}' exported! serviceConfig={}", service, config);
    }

    /**
     * 服务下线
     */
    public final void unExport() {
        if (!state.compareAndSet(EXPORTED_STATE, TERMINATED_STATE)) {
            return;
        }
        doUnExport();
        KinRpcRuntimeContext.removeService(this);

        log.info("service '{}' unExported!", config.getService());
    }

    /**
     * 发布服务
     */
    protected abstract void doExport();

    /**
     * 服务下线
     */
    protected abstract void doUnExport();

    //getter
    public ServiceConfig<T> getConfig() {
        return config;
    }
}
