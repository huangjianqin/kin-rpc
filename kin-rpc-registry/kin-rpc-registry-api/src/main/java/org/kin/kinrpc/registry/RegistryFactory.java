package org.kin.kinrpc.registry;

import org.kin.framework.utils.SPI;
import org.kin.kinrpc.config.RegistryConfig;

/**
 * 注册中心工厂
 *
 * @author huangjianqin
 * @date 2023/6/26
 */
@SPI("registryFactory")
@FunctionalInterface
public interface RegistryFactory {
    /**
     * 创建{@link Registry}实例
     *
     * @param config 注册中心配置
     * @return {@link Registry}实例
     */
    Registry create(RegistryConfig config);
}
