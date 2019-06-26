package org.kin.kinrpc.registry;

import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger("registry");

    protected final String serviceName;
    protected final int connectTimeout;
    protected final SerializerType serializerType;

    protected AbstractDirectory(String serviceName, int connectTimeout, SerializerType serializerType) {
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
