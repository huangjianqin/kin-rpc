package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;

import java.util.List;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistry extends AbstractRegistry {
    private List<String> hostAndPorts;
    private String serializerType;

    public DirectURLsRegistry(List<String> hostAndPorts, String serializerType) {
        this.hostAndPorts = hostAndPorts;
        this.serializerType = serializerType;
    }

    @Override
    public void connect(){
        //do nothing
    }

    @Override
    public void register(String serviceName, String host, int port){
        //do nothing
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        //do nothing
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        log.info("reference subscribe service '{}' ", serviceName);
        Directory directory = new DirectURLsDirectory(serviceName, connectTimeout, serializerType);
        directory.discover(hostAndPorts);
        return directory;
    }

    @Override
    public void unSubscribe(String serviceName) {
        log.info("reference unsubscribe service '{}' ", serviceName);
        Directory directory = DIRECTORY_CACHE.getIfPresent(serviceName);
        if(directory != null){
            directory.destroy();
        }
        DIRECTORY_CACHE.invalidate(serviceName);
    }

    @Override
    public void destroy() {
        hostAndPorts.clear();
        hostAndPorts = null;
        for(Directory directory: DIRECTORY_CACHE.asMap().values()){
            directory.destroy();
        }
        DIRECTORY_CACHE.invalidateAll();
    }
}
