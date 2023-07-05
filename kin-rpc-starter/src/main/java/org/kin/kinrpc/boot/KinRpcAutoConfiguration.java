package org.kin.kinrpc.boot;

import org.kin.kinrpc.conf.ZKRegistryConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author huangjianqin
 * @date 2020/12/13
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ZookeeperRegistryProperties.class})
public class KinRpcAutoConfiguration {
    @Autowired
    private ZookeeperRegistryProperties zookeeperRegistryProperties;

    @Bean
    @ConditionalOnProperty("kin.rpc.zookeeper.address")
    public ZKRegistryConfig zookeeperRegistryConfig() {
        return ZKRegistryConfig.create(zookeeperRegistryProperties.getAddress())
                .sessionTimeout(zookeeperRegistryProperties.getSessionTimeout())
                .build();
    }
}
