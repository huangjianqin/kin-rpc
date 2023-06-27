package org.kin.kinrpc.registry;

import org.kin.kinrpc.ServiceInstance;

import java.util.Collections;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public class DefaultServiceInstance implements ServiceInstance {
    /** 服务唯一标识 */
    private final String service;
    /** 服务实例schema */
    private final String scheme;
    /** 服务实例host */
    private final String host;
    /** 服务实例端口 */
    private final int port;
    /** 服务元数据 */
    private final Map<String, String> metadata;
    /** 服务权重 */
    private int weight;

    public DefaultServiceInstance(String service,
                                  String host,
                                  int port,
                                  Map<String, String> metadata) {
        this.service = service;
        this.host = host;
        this.port = port;
        this.metadata = Collections.unmodifiableMap(metadata);

        this.scheme = metadata(ServiceMetadataConstants.SCHEMA);
        this.weight = Integer.parseInt(metadata(ServiceMetadataConstants.WEIGHT, "0"));
    }

    @Override
    public String service() {
        return service;
    }

    @Override
    public String host() {
        return host;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public Map<String, String> metadata() {
        return metadata;
    }

    @Override
    public String scheme() {
        return scheme;
    }

    @Override
    public int weight() {
        return weight;
    }
}
