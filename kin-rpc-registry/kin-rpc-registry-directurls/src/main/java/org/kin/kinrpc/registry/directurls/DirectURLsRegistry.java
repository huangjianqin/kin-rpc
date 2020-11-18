package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * direct url, 直接根据给定的host port连接并调用服务
 * Created by huangjianqin on 2019/6/18.
 */
public final class DirectURLsRegistry extends AbstractRegistry {
    private static final Logger log = LoggerFactory.getLogger(DirectURLsRegistry.class);

    private List<Url> urls;

    public DirectURLsRegistry(Url url) {
        super(url);
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        this.urls = Arrays.asList(address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)).stream().map(Url::of).collect(Collectors.toList());
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
    public Directory subscribe(String serviceName) {
        log.info("reference subscribe service '{}' ", serviceName);
        Directory directory = new Directory(serviceName);
        directory.discover(url, urls);
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
