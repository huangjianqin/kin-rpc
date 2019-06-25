package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;

import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DefaultRegistry extends AbstractRegistry {
    private final Directory directory;

    public DefaultRegistry(String serviceName, int connectTimeout, List<HostAndPort> hostAndPorts) {
        directory = new DefaultDirectory(serviceName, connectTimeout, hostAndPorts);
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
        return directory;
    }

    @Override
    public void destroy() {
        directory.destroy();
    }
}
