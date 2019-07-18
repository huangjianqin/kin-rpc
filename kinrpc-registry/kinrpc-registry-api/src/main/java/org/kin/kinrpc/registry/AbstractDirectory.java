package org.kin.kinrpc.registry;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDirectory.class);
    //所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率
    private static final ThreadManager EXECUTOR = new ThreadManager(
            new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new SimpleThreadFactory("directory-discover")));
    static {
        JvmCloseCleaner.DEFAULT().add(() -> {
            EXECUTOR.shutdown();
        });
    }

    protected final String serviceName;
    protected final int connectTimeout;
    protected final SerializerType serializerType;
    protected volatile List<AbstractReferenceInvoker> invokers = Collections.emptyList();
    protected volatile boolean isStopped;

    protected AbstractDirectory(String serviceName, int connectTimeout, SerializerType serializerType) {
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
    }

    protected abstract void doDiscover(List<String> addresses);
    protected abstract void doDestroy();

    @Override
    public void discover(List<String> addresses) {
        EXECUTOR.execute(() -> doDiscover(addresses));
    }

    @Override
    public void destroy() {
        EXECUTOR.execute(() -> doDestroy());
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
