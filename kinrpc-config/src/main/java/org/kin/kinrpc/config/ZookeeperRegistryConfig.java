package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.HttpUtils;

/**
 * Created by 健勤 on 2017/2/13.
 */
class ZookeeperRegistryConfig extends AbstractRegistryConfig {
    public ZookeeperRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    public void check() {
        Preconditions.checkArgument(HttpUtils.checkHostPort(url),
                "zookeeper registry's host '" + url + "' format error");
    }
}
