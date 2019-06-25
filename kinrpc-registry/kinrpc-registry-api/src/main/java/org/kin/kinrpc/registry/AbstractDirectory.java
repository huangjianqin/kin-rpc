package org.kin.kinrpc.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger("registry");

    protected final String serviceName;
    protected final int connectTimeout;

    protected AbstractDirectory(String serviceName, int connectTimeout) {
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
