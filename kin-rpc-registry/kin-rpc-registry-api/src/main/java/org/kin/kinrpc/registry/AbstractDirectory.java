package org.kin.kinrpc.registry;

import org.kin.framework.actor.ActorLike;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory extends ActorLike<AbstractDirectory> implements Directory {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDirectory.class);

    protected final String serviceName;
    protected final int connectTimeout;
    protected final String serializerType;
    //所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率
    protected volatile List<ReferenceInvoker> invokers = Collections.emptyList();
    protected volatile boolean isStopped;

    protected AbstractDirectory(String serviceName, int connectTimeout, String serializerType) {
        super(RegistryThreadPool.THREADS);
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
    }

    protected abstract void doDiscover(List<String> addresses);
    protected abstract void doDestroy();

    @Override
    public void discover(List<String> addresses) {
        tell(directory -> doDiscover(addresses));
    }

    @Override
    public void destroy() {
        tell(directory -> doDestroy());
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
