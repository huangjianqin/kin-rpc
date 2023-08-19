package org.kin.kinrpc;

import java.util.Map;
import java.util.Objects;

/**
 * 代表服务实例集群的{@link ServiceInstance}实现
 *
 * @author huangjianqin
 * @date 2023/7/30
 */
public class ClusterServiceInstance implements ServiceInstance {
    /** 服务唯一id */
    private final int serviceId;
    /** 服务唯一标识 */
    private final String service;
    /** 注册中心类型 */
    private final String registryType;
    /** 注册中心地址 */
    private final String address;
    /** 注册中心权重 */
    private final int weight;

    /**
     * 用于多集群
     */
    public ClusterServiceInstance(int serviceId, String service, String registryType) {
        this(serviceId, service, registryType, "*", -1);
    }

    /**
     * 用于单集群
     */
    public ClusterServiceInstance(int serviceId, String service, String registryType, String address, int weight) {
        this.serviceId = serviceId;
        this.service = service;
        this.registryType = registryType;
        this.address = address;
        this.weight = weight;
    }

    @Override
    public String service() {
        return service;
    }

    @Override
    public String host() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int port() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<String, String> metadata() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String scheme() {
        return registryType;
    }

    @Override
    public int weight() {
        return weight;
    }

    @Override
    public boolean isCluster() {
        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ClusterServiceInstance)) return false;
        ClusterServiceInstance that = (ClusterServiceInstance) o;
        return serviceId == that.serviceId && Objects.equals(service, that.service) && Objects.equals(registryType, that.registryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(serviceId, service, registryType);
    }

    @Override
    public String toString() {
        return "ClusterServiceInstance{" +
                "serviceId=" + serviceId +
                ", service='" + service + '\'' +
                ", registryType='" + registryType + '\'' +
                ", address='" + address + '\'' +
                ", weight=" + weight +
                '}';
    }
}
