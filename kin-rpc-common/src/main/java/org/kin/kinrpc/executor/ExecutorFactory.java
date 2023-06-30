package org.kin.kinrpc.executor;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.ExecutorConfig;

/**
 * 服务调用线程池
 *
 * @author huangjianqin
 * @date 2021/4/20
 */
@SPI(alias = "executorFactory")
public interface ExecutorFactory {
    /**
     * 返回服务调用线程池
     * <p>
     * config 服务调用线程池配置
     *
     * @return 服务调用线程池
     */
    ManagedExecutor create(ExecutorConfig config);
}
