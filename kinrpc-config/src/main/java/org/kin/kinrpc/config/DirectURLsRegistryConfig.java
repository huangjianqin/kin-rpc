package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.HttpUtils;
import org.kin.kinrpc.common.Constants;

/**
 * Created by huangjianqin on 2019/6/20.
 */
class DirectURLsRegistryConfig extends AbstractRegistryConfig {
    DirectURLsRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    void check() {
        //包含多个直连url
        for(String split: address.split(Constants.DIRECT_URLS_REGISTRY_SPLITOR)){
            Preconditions.checkArgument(HttpUtils.checkHostPort(split),
                    "service's address '" + split + "' format error");
        }
    }
}
