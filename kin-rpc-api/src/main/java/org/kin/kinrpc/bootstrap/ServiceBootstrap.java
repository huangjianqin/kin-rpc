package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ServiceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(alias = "serviceBootstrap", singleton = false)
public abstract class ServiceBootstrap<T> {
    private static final Logger log = LoggerFactory.getLogger(ServiceBootstrap.class);
    /** 已发布的服务gsv */
    private static final Set<String> EXPORTED_SERVICES = new CopyOnWriteArraySet<>();

    /** 服务配置 */
    protected final ServiceConfig<T> config;

    protected ServiceBootstrap(ServiceConfig<T> config) {
        this.config = config;
    }

    /**
     * 发布服务
     */
    public final void export() {
        String service = config.service();
        if (EXPORTED_SERVICES.contains(service)) {
            log.warn("service '{}' has been exported before, " +
                    " please check and ensure duplicate export is right operation, " +
                    "ignore this if you did that on purpose!", service);
        }

        doExport();
    }

    /**
     * 服务下线
     */
    public final void unExport() {
        doUnExport();
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
