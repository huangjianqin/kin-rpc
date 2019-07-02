package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.HttpUtils;

/**
 * Created by 健勤 on 2017/2/13.
 */
class ZookeeperRegistryConfig extends AbstractRegistryConfig {
    ZookeeperRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    void check() {
        Preconditions.checkArgument(HttpUtils.checkHostPort(address),
                "zookeeper registry's host '" + address + "' format error");
    }
}
