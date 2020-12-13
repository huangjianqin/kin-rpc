package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * @author huangjianqin
 * @date 2020/8/13
 */
public class RedisRegistryConfig extends AbstractRegistryConfig {
    /** 观察服务变化间隔, 目前仅用于redis */
    private long watchInterval;

    RedisRegistryConfig(String adress) {
        super(adress);
        setWatchInterval(Constants.WATCH_INTERVAL);
    }

    @Override
    void check() {
        super.check();
        Preconditions.checkArgument(watchInterval > 0,
                "redis watchInterval '".concat(watchInterval + "").concat("' must greater than zero"));
    }

    //setter && getter

    public long getWatchInterval() {
        return watchInterval;
    }

    public void setWatchInterval(long watchInterval) {
        this.watchInterval = watchInterval;
    }

    //--------------------------builder--------------------------

    /** 获取redis注册中心配置builder */
    public static RedisRegistryBuilder create(String address) {
        return new RedisRegistryBuilder(address);
    }

    public static class RedisRegistryBuilder {
        private RedisRegistryConfig registryConfig;

        private RedisRegistryBuilder(String address) {
            this.registryConfig = new RedisRegistryConfig(address);
        }

        public RedisRegistryBuilder watchInterval(long watchInterval) {
            registryConfig.watchInterval = watchInterval;
            return this;
        }

        public RedisRegistryConfig build() {
            return registryConfig;
        }
    }
}
