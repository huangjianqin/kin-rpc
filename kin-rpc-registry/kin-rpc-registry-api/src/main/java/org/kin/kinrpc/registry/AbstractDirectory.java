package org.kin.kinrpc.registry;

import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2019/6/11
 */
public abstract class AbstractDirectory implements Directory {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDirectory.class);
    protected final String serviceName;
    protected final int connectTimeout;
    protected final String serializerType;
    protected final boolean compression;

    /** 所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率 */
    private volatile List<ReferenceInvoker> invokers = Collections.emptyList();
    private volatile boolean isStopped;

    protected AbstractDirectory(String serviceName, int connectTimeout, String serializerType, boolean compression) {
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
        this.compression = compression;
    }


    private List<ReferenceInvoker> getActiveReferenceInvoker() {
        return invokers.stream().filter(ReferenceInvoker::isActive).collect(Collectors.toList());
    }

    private void updateInvokers(List<ReferenceInvoker> newInvokers) {
        invokers = Collections.unmodifiableList(newInvokers);
    }

    /**
     * 获取当前可用invokers
     */
    @Override
    public List<ReferenceInvoker> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            return getActiveReferenceInvoker();
        }
        return Collections.emptyList();
    }

    /**
     * 连接发现的invokers
     *
     * @param addresses invokers address
     * @return invokers
     */
    protected abstract List<ReferenceInvoker> doDiscover(List<String> addresses, List<ReferenceInvoker> originInvokers);

    /**
     * invoker directory销毁
     */
    protected abstract void doDestroy();

    @Override
    public void discover(List<String> addresses) {
        if (!isStopped) {
            updateInvokers(doDiscover(addresses, new ArrayList<>(invokers)));
        }
    }

    @Override
    public void destroy() {
        if (!isStopped) {
            isStopped = true;
            doDestroy();
            for (ReferenceInvoker invoker : invokers) {
                invoker.shutdown();
            }
            invokers = null;
            log.info("zookeeper directory destroyed");
        }
    }

    @Override
    public String getServiceName() {
        return serviceName;
    }
}
