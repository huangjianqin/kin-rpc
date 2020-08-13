package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * direct url, 直接根据给定的host port连接并调用服务
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DirectURLsRegistry.class);

    private List<String> hostAndPorts;
    private final String serializerType;
    private final boolean compression;

    public DirectURLsRegistry(List<String> hostAndPorts, String serializerType, boolean compression) {
        this.hostAndPorts = hostAndPorts;
        this.serializerType = serializerType;
        this.compression = compression;
    }

    @Override
    public void connect() {
        //do nothing
    }

    @Override
    public void register(String serviceName, String host, int port) {
        //do nothing
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        //do nothing
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("reference subscribe service '{}' ", serviceName);
        Directory directory = new Directory(serviceName, connectTimeout, serializerType, compression);
        directory.discover(hostAndPorts);
        directoryCache.put(serviceName, directory);
        return directory;
    }

    @Override
    public void unSubscribe(String serviceName) {
        log.info("reference unsubscribe service '{}' ", serviceName);
        Directory directory = directoryCache.getIfPresent(serviceName);
        if (directory != null) {
            directory.destroy();
        }
        directoryCache.invalidate(serviceName);
    }

    @Override
    public void destroy() {
        hostAndPorts.clear();
        hostAndPorts = null;
        for (Directory directory : directoryCache.asMap().values()) {
            directory.destroy();
        }
        directoryCache.invalidateAll();
    }
}
