package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.SerializationType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
@ConfigurationProperties("kin.kinrpc")
public class KinRpcProperties {
    /** 服务所属组 */
    private String group = "kinrpc";
    /** 版本号 */
    private String version = "0.1.0.0";
    /** 注册中心url */
    private List<String> registries;
    private String serialization = SerializationType.JSON.getName();
    private ProviderProperties provider;
    private ConsumerProperties consumer;
    // TODO: 2023/7/6 ssl

}
