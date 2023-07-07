package org.kin.kinrpc.config;

/**
 * reference group config, 一组服务引用通用配置, 减少配置工作量
 * 通过{@link #getGroup()}区分不同的组
 *
 * @author huangjianqin
 * @date 2023/7/7
 */
public class ConsumerConfig extends AbstractReferenceConfig<ConsumerConfig> {
    public static ConsumerConfig create() {
        return new ConsumerConfig();

    }

    private ConsumerConfig() {
    }
}
