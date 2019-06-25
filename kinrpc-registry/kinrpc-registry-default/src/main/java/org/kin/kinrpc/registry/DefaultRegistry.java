package org.kin.kinrpc.registry;

import com.google.common.base.Preconditions;
import com.google.common.net.HostAndPort;

import java.util.List;
import java.util.zip.DataFormatException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DefaultRegistry extends AbstractRegistry {
    private List<HostAndPort> hostAndPorts;

    public DefaultRegistry(List<HostAndPort> hostAndPorts) {
        this.hostAndPorts = hostAndPorts;
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
        return new DefaultDirectory(serviceName, connectTimeout, hostAndPorts);
    }

    @Override
    public void destroy() {
        hostAndPorts.clear();
        hostAndPorts = null;
    }
}
