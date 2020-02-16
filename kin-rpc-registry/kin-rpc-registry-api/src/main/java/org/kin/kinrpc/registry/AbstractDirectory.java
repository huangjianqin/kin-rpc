package org.kin.kinrpc.registry;

import org.kin.framework.actor.ActorLike;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/6/11.
 */
public abstract class AbstractDirectory extends ActorLike<AbstractDirectory> implements Directory {
    protected static final Logger log = LoggerFactory.getLogger(AbstractDirectory.class);

    protected final String serviceName;
    protected final int connectTimeout;
    protected final String serializerType;
    protected final boolean compression;

    //所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率
    protected volatile List<ReferenceInvoker> invokers = Collections.emptyList();
    protected volatile boolean isStopped;

    private volatile short waiters;

    protected AbstractDirectory(String serviceName, int connectTimeout, String serializerType, boolean compression) {
        super(RegistryThreadPool.THREADS);
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
        this.compression = compression;
    }

    /**
     * 获取当前可用invokers
     */
    @Override
    public List<ReferenceInvoker> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            //等待invokers不为空
            if(CollectionUtils.isEmpty(invokers)){
                synchronized (this){
                    waiters++;
                    try{
                        wait();
                    } catch (InterruptedException e) {

                    } finally {
                        waiters--;
                    }
                }
            }
            return invokers.stream().filter(ReferenceInvoker::isActive).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    protected abstract List<ReferenceInvoker> doDiscover(List<String> addresses);

    protected abstract void doDestroy();

    @Override
    public void discover(List<String> addresses) {
        tell(directory -> {
            invokers = doDiscover(addresses);
            if(waiters > 0){
                synchronized (this){
                    notifyAll();
                }
            }
        });
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
