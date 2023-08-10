package org.kin.kinrpc.config;

/**
 * service group config, 一组服务通用配置, 减少配置工作量
 * 通过{@link #getGroup()}区分不同的组
 *
 * @author huangjianqin
 * @date 2023/7/7
 */
public class ProviderConfig extends AbstractServiceConfig<ProviderConfig> {
    public static ProviderConfig create() {
        return new ProviderConfig();

    }

    protected ProviderConfig() {
    }

    @Override
    public String toString() {
        return "ProviderConfig{" + super.toString() + "}";
    }
}
