package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * direct url, 直接根据给定的host port连接并调用服务
 * Created by huangjianqin on 2019/6/18.
 */
public final class DirectURLsRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DirectURLsRegistry.class);

    private List<Url> urls;

    public DirectURLsRegistry(List<Url> urls) {
        this.urls = urls;
    }

    @Override
    public void connect() {
        //do nothing
    }

    @Override
    public void register(Url url) {
        //do nothing
    }

    @Override
    public void unRegister(Url url) {
        //do nothing
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("reference subscribe service '{}' ", serviceName);
        Directory directory = new Directory(serviceName, connectTimeout);
        directory.discover(urls);
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
        urls.clear();
        urls = null;
        for (Directory directory : directoryCache.asMap().values()) {
            directory.destroy();
        }
        directoryCache.invalidateAll();
    }
}
