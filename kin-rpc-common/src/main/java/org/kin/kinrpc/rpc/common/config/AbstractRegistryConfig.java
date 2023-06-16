package org.kin.kinrpc.rpc.common.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.NetUtils;

/**
 * Created by huangjianqin on 2019/6/18.
 */
public abstract class AbstractRegistryConfig extends AbstractConfig {
    /** 注册中心地址 */
    protected String address;

    AbstractRegistryConfig(String address) {
        this.address = address;
    }

    @Override
    void check() {
        Preconditions.checkArgument(NetUtils.checkHostPort(address),
                "redis address '".concat(address).concat("' format error"));
    }

    //setter && getter
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }
}
