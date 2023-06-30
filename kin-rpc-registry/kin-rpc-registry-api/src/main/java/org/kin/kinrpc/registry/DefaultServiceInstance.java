package org.kin.kinrpc.registry;

import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.utils.GsvUtils;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/6/27
 */
public class DefaultServiceInstance implements ServiceInstance {
    /** 服务唯一id */
    private final int serviceId;
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
        this.serviceId = GsvUtils.serviceId(service);
        this.service = service;
        this.host = host;
        this.port = port;
        this.metadata = Collections.unmodifiableMap(metadata);

        this.scheme = metadata(ServiceMetadataConstants.SCHEMA_KEY);
        this.weight = Integer.parseInt(metadata(ServiceMetadataConstants.WEIGHT_KEY, "0"));
    }

    @Override
    public int serviceId() {
        return serviceId;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefaultServiceInstance that = (DefaultServiceInstance) o;
        return serviceId == that.serviceId && port == that.port && Objects.equals(host, that.host);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, host, port);
    }
}
