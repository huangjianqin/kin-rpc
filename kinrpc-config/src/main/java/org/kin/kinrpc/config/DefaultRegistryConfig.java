package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.HttpUtils;
import org.kin.kinrpc.common.Constants;

/**
 * Created by huangjianqin on 2019/6/20.
 */
class DefaultRegistryConfig extends AbstractRegistryConfig {
    public DefaultRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    public void check() {
        //包含多个直连url
        for(String split: url.split(Constants.DEFAULT_REGISTRY_URL_SPLITOR)){
            Preconditions.checkArgument(HttpUtils.checkHostPort(split),
                    "service's url '" + split + "' format error");
        }
    }
}
