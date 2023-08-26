package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.ApplicationContext;
import org.kin.kinrpc.config.ReferenceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * reference bootstrap抽象父类
 *
 * @author huangjianqin
 * @date 2023/6/20
 */
@SPI(alias = "referenceBootstrap", singleton = false)
public abstract class ReferenceBootstrap<T> {
    private static final Logger log = LoggerFactory.getLogger(ReferenceBootstrap.class);
    /** 已引用的服务gsv */
    private static final Set<String> REFERENCED_SERVICES = new CopyOnWriteArraySet<>();
    /** 初始状态 */
    private static final byte INIT_STATE = 1;
    /** refer状态 */
    private static final byte REFERENCE_STATE = 2;
    /** unRefer后状态 */
    private static final byte TERMINATED_STATE = 3;

    /** 服务引用配置 */
    protected final ReferenceConfig<T> config;
    /** 状态 */
    private final AtomicInteger state = new AtomicInteger(INIT_STATE);
    /** service reference */
    protected volatile T reference;

    protected ReferenceBootstrap(ReferenceConfig<T> config) {
        this.config = config;
    }

    /**
     * 创建服务引用代理实例
     *
     * @return 服务引用代理实例
     */
    public final T refer() {
        if (!state.compareAndSet(INIT_STATE, REFERENCE_STATE)) {
            return reference;
        }

        String service = config.getService();
        if (REFERENCED_SERVICES.contains(service)) {
            throw new IllegalStateException("service '{}' has been referenced before");
        }

        REFERENCED_SERVICES.add(service);
        reference = doRefer();
        ApplicationContext.instance().cacheReference(this);

        if (log.isDebugEnabled()) {
            log.debug("refer service '{}'. referenceConfig={}", config.getService(), config);
        }
        return reference;
    }

    /**
     * 取消服务引用
     */
    public final void unRefer() {
        if (!state.compareAndSet(REFERENCE_STATE, TERMINATED_STATE)) {
            return;
        }

        doUnRefer();
        //释放引用
        reference = null;

        ApplicationContext.instance().removeReference(this);

        if (log.isDebugEnabled()) {
            log.debug("unRefer service '{}'", config.getService());
        }
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
