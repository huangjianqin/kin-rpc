package org.kin.kinrpc.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger("registry");

    protected final Class<?> interfaceClass;
    protected final int connectTimeout;

    protected AbstractDirectory(Class<?> interfaceClass, int connectTimeout) {
        this.interfaceClass = interfaceClass;
        this.connectTimeout = connectTimeout;
    }

    @Override
    public String getServiceName() {
        return interfaceClass.getName();
    }
}
