package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.common.URL;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public class DefaultRegistryFactory extends AbstractRegistryFactory{
    @Override
    public Registry getRegistry(URL url) {
        String address = url.getParam(Constants.REGISTRY_URL);

        List<HostAndPort> hostAndPorts = new ArrayList<>();
        for(String one: address.split(";")){
            hostAndPorts.add(HostAndPort.fromString(one));
        }

        try {
            Registry registry = registryCache.get(address, () -> new DefaultRegistry(hostAndPorts));
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            ExceptionUtils.log(e);
        }

        return null;
    }
}
