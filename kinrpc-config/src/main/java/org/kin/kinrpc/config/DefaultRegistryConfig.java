package org.kin.kinrpc.config;

import com.google.common.base.Preconditions;
import org.kin.framework.utils.HttpUtils;

/**
 * Created by huangjianqin on 2019/6/20.
 */
class DefaultRegistryConfig extends RegistryConfig {
    public DefaultRegistryConfig(String adress) {
        super(adress);
    }

    @Override
    public void check() {
        //包含多个直连url
        for(String split: url.split(";")){
            Preconditions.checkArgument(HttpUtils.checkHostPort(split),
                    "service's url '" + split + "' format error");
        }
    }
}
