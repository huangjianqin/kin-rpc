package org.kin.kinrpc.rpc.common.config;

import org.kin.kinrpc.rpc.common.constants.Constants;
import org.kin.kinrpc.rpc.common.Url;

/**
 * Created by huangjianqin on 2019/6/20.
 */
public class DirectRegistryConfig extends AbstractRegistryConfig {
    public DirectRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    void check() {
        //包含多个直连url
        for (String split : address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)) {
            try {
                //检查url格式
                Url.of(split);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
        }
    }
}
