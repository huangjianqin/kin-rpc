package org.kin.kinrpc.registry.redis;

import org.kin.framework.log.LoggerOprs;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.registry.AbstractRegistryFactory;
import org.kin.kinrpc.registry.Registry;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;

import java.util.concurrent.ExecutionException;

/**
 * redis注册中心工厂
 *
 * @author huangjianqin
 * @date 2020/8/12
 */
public class RedisRegistryFactory extends AbstractRegistryFactory implements LoggerOprs {
    @Override
    public Registry getRegistry(Url url) {
        String address = url.getParam(Constants.REGISTRY_URL_KEY);
        //解析地址
        Object[] addressParseResult = NetUtils.parseIpPort(address);
        String host = addressParseResult[0].toString();
        int port = Integer.parseInt(addressParseResult[1].toString());

        long sessionTimeout = Long.parseLong(url.getParam(Constants.SESSION_TIMEOUT_KEY));
        long watchInterval = Long.parseLong(url.getParam(Constants.WATCH_INTERVAL_KEY));

        try {
            Registry registry = REGISTRY_CACHE.get(address, () -> new RedisRegistry(host, port, sessionTimeout, watchInterval));
            registry.connect();
            registry.retain();
            return registry;
        } catch (ExecutionException e) {
            log.error(e.getMessage(), e);
        }

        return null;
    }
}
