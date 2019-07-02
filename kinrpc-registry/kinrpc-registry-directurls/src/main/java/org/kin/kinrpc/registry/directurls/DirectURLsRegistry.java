package org.kin.kinrpc.registry.directurls;

import org.kin.kinrpc.registry.AbstractRegistry;
import org.kin.kinrpc.registry.Directory;
import org.kin.kinrpc.rpc.serializer.SerializerType;

import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DirectURLsRegistry extends AbstractRegistry {
    private List<String> hostAndPorts;
    private SerializerType serializerType;

    public DirectURLsRegistry(List<String> hostAndPorts, SerializerType serializerType) {
        this.hostAndPorts = hostAndPorts;
        this.serializerType = serializerType;
    }

    @Override
    public void connect() throws DataFormatException {
        //do nothing
    }

    @Override
    public void register(String serviceName, String host, int port) throws DataFormatException {
        //do nothing
    }

    @Override
    public void unRegister(String serviceName, String host, int port) {
        //do nothing
    }

    @Override
    public Directory subscribe(String serviceName, int connectTimeout) {
        Directory directory = new DirectURLsDirectory(serviceName, connectTimeout, serializerType);
        directory.discover(hostAndPorts);
        return directory;
    }

    @Override
    public void destroy() {
        hostAndPorts.clear();
        hostAndPorts = null;
    }
}
