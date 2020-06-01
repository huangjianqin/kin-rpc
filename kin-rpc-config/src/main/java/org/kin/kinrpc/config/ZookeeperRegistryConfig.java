package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * Created by 健勤 on 2017/2/13.
 */
class ZookeeperRegistryConfig extends AbstractRegistryConfig {
    ZookeeperRegistryConfig(String adress) {
        super(adress);
        //连接注册中心的会话超时,以毫秒算,默认5s
        setSessionTimeout(Constants.ZOOKEEPER_SESSION_TIMEOUT);
    }

    @Override
    void check() {
        Preconditions.checkArgument(NetUtils.checkHostPort(address),
                "zookeeper registry's host '" + address + "' format error");
    }
}
