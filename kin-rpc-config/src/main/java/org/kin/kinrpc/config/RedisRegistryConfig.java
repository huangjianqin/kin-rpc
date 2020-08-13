package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * @author huangjianqin
 * @date 2020/8/13
 */
public class RedisRegistryConfig extends AbstractRegistryConfig {
    RedisRegistryConfig(String adress) {
        super(adress);
        //连接注册中心的会话超时,以毫秒算,默认5s
        setSessionTimeout(Constants.SESSION_TIMEOUT);
        setWatchInterval(Constants.WATCH_INTERVAL);
    }

    @Override
    void check() {
        Preconditions.checkArgument(NetUtils.checkHostPort(address),
                "redis address '".concat(address).concat("' format error"));
        Preconditions.checkArgument(sessionTimeout > 0,
                "redis sessionTimeout '".concat(sessionTimeout + "").concat("' must greater than zero"));
        Preconditions.checkArgument(watchInterval > 0,
                "redis watchInterval '".concat(watchInterval + "").concat("' must greater than zero"));
    }
}
