package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * @author huangjianqin
 * @date 2019/7/3
 */
public class Zookeeper2RegistryConfig extends AbstractRegistryConfig {
    Zookeeper2RegistryConfig(String adress) {
        super(adress);
        //连接注册中心的会话超时,以毫秒算,默认5s
        setSessionTimeout(Constants.ZOOKEEPER_SESSION_TIMEOUT);
    }

    @Override
    void check() {
        Preconditions.checkArgument(NetUtils.checkHostPort(address),
                "zookeeper2 registry's host '" + address + "' format error");
    }
}
