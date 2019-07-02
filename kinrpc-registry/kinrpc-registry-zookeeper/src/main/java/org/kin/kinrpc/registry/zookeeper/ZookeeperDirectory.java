package org.kin.kinrpc.registry.zookeeper;

import com.google.common.net.HostAndPort;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.registry.AbstractDirectory;
import org.kin.kinrpc.registry.common.RegistryConstants;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvokerImpl;
import org.kin.kinrpc.rpc.serializer.SerializerType;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/13.
 * 以zookeeper为注册中心, 实时监听服务状态变化, 并更新可调用服务invoker
 */
public class ZookeeperDirectory extends AbstractDirectory {
    private final ZookeeperRegistry registry;
    //无效invoker由zookeeper注册中心控制, 所以可能会存在list有无效invoker(zookeeper没有及时更新到)
    private List<AbstractReferenceInvoker> invokers;

    //用于在没有invoker可用时阻塞
    private Lock lock = new ReentrantLock();
    private Condition hasInvokers = lock.newCondition();
    private volatile boolean isStopped;

    public ZookeeperDirectory(String serviceName, int connectTimeout, ZookeeperRegistry registry, SerializerType serializerType) {
        super(serviceName, connectTimeout, serializerType);
        this.registry = registry;
    }

    /**
     * 获取当前可用的所有ReferenceInvoker
     */
    @Override
    public List<AbstractReferenceInvoker> list() {
        lock.lock();
        try {
            //第一次调用
            if (invokers == null) {
                this.invokers = new ArrayList<>();
                discover();
            }

            //Directory关闭中调用该方法会返回一个size=0的列表
            List<AbstractReferenceInvoker> shallowClonedInvokers = Collections.emptyList();
            while (!isStopped) {
                try {
                    waitingForAvailableInvoker();
                    shallowClonedInvokers = new ArrayList<>(this.invokers);
                    if (shallowClonedInvokers.size() > 0) {
                        break;
                    }
                } catch (InterruptedException e) {

                }
            }

            return shallowClonedInvokers.stream().filter(AbstractReferenceInvoker::isActive).collect(Collectors.toList());
        } finally {
            lock.unlock();
        }
    }

    /**
     * 如果可用的ReferenceInvoker,释放等待可用ReferenceInvoker的线程
     */
    private void signalAvailableInvoker() {
        lock.lock();
        try {
            hasInvokers.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 若没有可用ReferenceInvoker,等待一段时间
     *
     * @return
     * @throws InterruptedException
     */
    private void waitingForAvailableInvoker() throws InterruptedException {
        lock.lock();
        try {
            if (this.invokers.size() <= 0) {
                hasInvokers.await(300, TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 发现服务,发现可用服务的address
     */
    private void discover() {
        if (!isStopped) {
            log.info("reference discover service...");
            ZooKeeper zooKeeper = registry.getConnection();
            try {
                //获取注册服务的所有address
                List<String> znodeList = zooKeeper.getChildren(RegistryConstants.getPath(getServiceName()),
                        (WatchedEvent watchedEvent) -> {
                            if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                                log.info("service '" + getServiceName() + "' server node changed");
                                discover();
                            }
                        });

                //更新当前的invoker
                updateCurrentInvoker(znodeList);
            } catch (KeeperException e) {
                //如果不存在该节点,则表明服务取消注册了
                if (e.code().equals(KeeperException.Code.NONODE)) {
                    log.error("service '" + getServiceName() + "' unregisted");
                    //等待一段时间
                    try {
                        Thread.sleep(5 * 1000L);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    discover();
                }
            } catch (InterruptedException e) {
                log.error("", e);
            }
        }
    }

    private void updateCurrentInvoker(List<String> addresses) {
        if (isStopped) {
            return;
        }

        log.info("update current invoker...");
        StringBuilder sb = new StringBuilder();
        List<HostAndPort> hostAndPorts = new ArrayList<>();
        if(addresses != null && addresses.size() > 0){
            for (String address : addresses) {
                HostAndPort hostAndPort = HostAndPort.fromString(address);
                hostAndPorts.add(hostAndPort);

                sb.append(hostAndPort.toString() + ", ");
            }
        }
        log.info("current service server address: " + sb.toString());

        lock.lock();
        List<AbstractReferenceInvoker> invalidInvokers = new ArrayList<>();
        try {
            if (!isStopped) {
                if (hostAndPorts.size() > 0) {
                    //该循环处理完后,addresses里面的都是新的
                    for (AbstractReferenceInvoker invoker : invokers) {
                        //不包含该连接,而是连接变为无用,shutdown
                        HostAndPort invokerHostAndPort = invoker.getAddress();
                        if (!hostAndPorts.contains(invokerHostAndPort)) {
                            invalidInvokers.add(invoker);
                        } else {
                            //连接仍然有效,故addresses仍然有该连接的host:port
                            hostAndPorts.remove(invokerHostAndPort);
                        }
                    }
                    //清除掉invokers中的无效Invoker
                    invokers.removeAll(invalidInvokers);
                } else {
                    //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
                    invalidInvokers.addAll(this.invokers);
                    this.invokers.clear();
                }
            } else {
                hostAndPorts.clear();
            }
        } finally {
            lock.unlock();
        }

        //new ReferenceInvokers
        for (HostAndPort hostAndPort : hostAndPorts) {
            //address有效,创建ReferenceInvoker
            connectServer(hostAndPort.getHost(), hostAndPort.getPort());
        }

        //remove invalid ReferenceInvokers
        for (AbstractReferenceInvoker invoker : invalidInvokers) {
            invoker.shutdown();
        }

        log.info("invokers updated");
    }

    /**
     * 创建新的ReferenceInvoker,连接Service Server
     */
    private void connectServer(String host, int port) {
        ThreadManager.DEFAULT.submit(() -> {
            //创建连接
            RPCReference rpcReference = new RPCReference(new InetSocketAddress(host, port), serializerType.newInstance(), connectTimeout);
            AbstractReferenceInvoker refereneceInvoker = new ReferenceInvokerImpl(serviceName, rpcReference);
            //真正启动连接
            refereneceInvoker.init();

            if (!isStopped) {
                lock.lock();
                try {
                    if (!isStopped) {
                        invokers.add(refereneceInvoker);
                        signalAvailableInvoker();
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    @Override
    public void destroy() {
        isStopped = true;
        //关闭注册中心连接,此时就不会再更新invokers列表,故不用加锁
        registry.destroy();
        //关闭所有当前连接
        lock.lock();
        try {
            if (invokers != null && invokers.size() > 0) {
                for (AbstractReferenceInvoker invoker : invokers) {
                    invoker.shutdown();
                }
                invokers.clear();
            }
        } finally {
            lock.unlock();
        }
        //释放所有等待idle invoker的线程
        this.signalAvailableInvoker();

        log.info("zookeeper directory destroyed");
    }

}
