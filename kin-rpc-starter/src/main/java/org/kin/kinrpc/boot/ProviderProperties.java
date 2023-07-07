package org.kin.kinrpc.boot;

import org.kin.kinrpc.config.SerializationType;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/7/6
 */
public class ProviderProperties {
    // TODO: 2023/7/6 executor
    /** 传输层配置 */
    private List<String> servers;
    private String serialization = SerializationType.JSON.getName();
}
