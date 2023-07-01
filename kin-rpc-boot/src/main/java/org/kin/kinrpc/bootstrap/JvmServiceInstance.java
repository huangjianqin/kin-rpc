package org.kin.kinrpc.bootstrap;

import org.kin.kinrpc.ServiceInstance;
import org.kin.kinrpc.config.ProtocolType;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/2
 */
public class JvmServiceInstance implements ServiceInstance {
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
}
