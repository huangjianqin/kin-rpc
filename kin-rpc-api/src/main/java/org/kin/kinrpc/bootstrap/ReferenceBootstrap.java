package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(alias = "referenceBootstrap", singleton = false)
public abstract class ReferenceBootstrap<T> {
    private static final Logger log = LoggerFactory.getLogger(ServiceBootstrap.class);
    /** 已引用的服务gsv */
    private static final Set<String> REFERENCED_SERVICES = new CopyOnWriteArraySet<>();

    /** 服务引用配置 */
    protected final ReferenceConfig<T> config;

    protected ReferenceBootstrap(ReferenceConfig<T> config) {
        this.config = config;
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    public final T refer() {
        String service = config.service();
        if (REFERENCED_SERVICES.contains(service)) {
            log.warn("service '{}' has been referenced before, " +
                    " please check and ensure duplicate refer is right operation, " +
                    "ignore this if you did that on purpose!", service);
        }

        return doRefer();
    }

    /**
     * 取消服务引用
     */
    public final void unRefer() {
        doUnRefer();
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    public abstract T doRefer();

    /**
     * 取消服务引用
     */
    public abstract void doUnRefer();

    //getter
    public ReferenceConfig<T> getConfig() {
        return config;
    }
}
