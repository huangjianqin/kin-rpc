package org.kin.kinrpc.spring.starter;

import org.kin.kinrpc.config.RedisRegistryConfig;
import org.kin.kinrpc.config.ZookeeperRegistryConfig;
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
@EnableConfigurationProperties({ZookeeperRegistryProperties.class, RedisRegistryProperties.class})
public class KinRpcAutoConfiguration {
    @Autowired
    private ZookeeperRegistryProperties zookeeperRegistryProperties;
    @Autowired
    private RedisRegistryProperties redisRegistryProperties;

    @Bean
    @ConditionalOnProperty("kin.rpc.zookeeper.address")
    public ZookeeperRegistryConfig zookeeperRegistryConfig() {
        return ZookeeperRegistryConfig.create(zookeeperRegistryProperties.getAddress())
                .sessionTimeout(zookeeperRegistryProperties.getSessionTimeout())
                .build();
    }

    @Bean
    @ConditionalOnProperty("kin.rpc.redis.address")
    public RedisRegistryConfig registryConfig() {
        return RedisRegistryConfig.create(redisRegistryProperties.getAddress())
                .watchInterval(redisRegistryProperties.getWatchInterval())
                .build();
    }
}
