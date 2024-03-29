package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ProtocolType;

import java.util.Map;
import java.util.Objects;

/**
 * jvm模式下的服务实例信息
 *
 * @author huangjianqin
 * @date 2023/7/2
 */
public class JvmServiceInstance implements ServiceInstance {
    /** 服务唯一标识 */
    private final String service;

    public JvmServiceInstance(String service) {
        this.service = service;
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
        return ProtocolType.JVM.getName();
    }

    @Override
    public int weight() {
        return 0;
    }

    @Override
    public boolean isCluster() {
        return false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JvmServiceInstance that = (JvmServiceInstance) o;
        return Objects.equals(service, that.service);
    }

    @Override
    public int hashCode() {
        return Objects.hash(service);
    }
}
