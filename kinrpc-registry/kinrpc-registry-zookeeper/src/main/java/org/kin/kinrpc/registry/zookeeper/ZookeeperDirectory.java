package org.kin.kinrpc.registry.zookeeper;

import com.google.common.net.HostAndPort;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.kinrpc.registry.AbstractDirectory;
import org.kin.kinrpc.registry.RegistryConstants;
import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.rpc.invoker.impl.JavaReferenceInvoker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
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
    private List<ReferenceInvoker> invokers;

    //用于在没有invoker可用时阻塞
    private Lock lock = new ReentrantLock();
    private Condition hasInvokers = lock.newCondition();
    private volatile boolean isRunning = true;

    public ZookeeperDirectory(Class<?> interfaceClass, int connectTimeout, ZookeeperRegistry registry) {
        super(interfaceClass, connectTimeout);
        this.registry = registry;
    }

    /**
     * 获取当前可用的所有ReferenceInvoker
     */
    @Override
    public List<ReferenceInvoker> list() {
        lock.lock();
        try {
            //第一次调用
            if (invokers == null) {
                this.invokers = new ArrayList<>();
                discover();
            }

            //Directory关闭中调用该方法会返回一个size=0的列表
            List<ReferenceInvoker> shallowClonedInvokers = new ArrayList<>();
            while (isRunning) {
                try {
                    waitingForAvailableInvoker();
                    shallowClonedInvokers = new ArrayList<>(this.invokers);
                    if (shallowClonedInvokers.size() > 0) {
                        break;
                    }
                } catch (InterruptedException e) {
                    ExceptionUtils.log(e);
                }
            }

            return shallowClonedInvokers.stream().filter(ReferenceInvoker::isActive).collect(Collectors.toList());
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
                hasInvokers.await(200, TimeUnit.MILLISECONDS);
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 发现服务,发现可用服务的address
     */
    private void discover() {
        if (isRunning) {
            log.info("consumer discover service...");
            ZooKeeper zooKeeper = registry.getConnection();
            try {
                //获取注册服务的所有address
                List<String> znodeList = zooKeeper.getChildren(RegistryConstants.getPath(getServiceName()),
                        (WatchedEvent watchedEvent) -> {
                            if (watchedEvent.getType() == Watcher.Event.EventType.NodeChildrenChanged) {
                                log.info("service '" + getServiceName() + "' Server node changed");
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
                ExceptionUtils.log(e);
            }
        }
    }

    private void updateCurrentInvoker(List<String> addresses) {
        if (addresses == null) {
            return;
        }

        log.info("update current invoker...");
        StringBuilder sb = new StringBuilder();
        List<HostAndPort> hostAndPorts = new ArrayList<>();
        for (String address : addresses) {
            HostAndPort hostAndPort = HostAndPort.fromString(address);
            hostAndPorts.add(hostAndPort);

            sb.append(hostAndPort.toString() + ", ");
        }
        log.info("current address: " + sb.toString());

        lock.lock();
        List<ReferenceInvoker> invalidInvoker = new ArrayList<ReferenceInvoker>();
        try {
            if (isRunning) {
                if (hostAndPorts.size() > 0) {
                    //该循环处理完后,addresses里面的都是新的
                    for (ReferenceInvoker invoker : invokers) {
                        //不包含该连接,而是连接变为无用,shutdown
                        HostAndPort invokerHostAndPort = invoker.getAddress();
                        if (!hostAndPorts.contains(invokerHostAndPort)) {
                            invalidInvoker.add(invoker);
                        } else {
                            //连接仍然有效,故addresses仍然有该连接的host:port
                            hostAndPorts.remove(invokerHostAndPort);
                        }
                    }
                    //清除掉invokers中的无效Invoker
                    invokers.removeAll(invalidInvoker);
                } else {
                    //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
                    invalidInvoker.addAll(this.invokers);
                    this.invokers.clear();
                }
            } else {
                hostAndPorts.clear();
            }
        } finally {
            lock.unlock();
        }

        //构建新的ReferenceInvoker
        for (HostAndPort hostAndPort : hostAndPorts) {
            //address有效,创建ReferenceInvoker
            connectServer(hostAndPort.getHost(), hostAndPort.getPort());
        }

        //移除所有无效的Service Server
        for (ReferenceInvoker invoker : invalidInvoker) {
            invoker.shutdown();
        }
    }

    /**
     * 创建新的ReferenceInvoker,连接Service Server
     */
    private void connectServer(String host, int port) {
        ThreadManager.DEFAULT.submit(() -> {
            //创建连接
            RPCReference rpcReference = new RPCReference(new InetSocketAddress(host, port));
            ReferenceInvoker refereneceInvoker = new JavaReferenceInvoker(interfaceClass, rpcReference);
            //真正启动连接
            refereneceInvoker.init();

            if (isRunning && refereneceInvoker.isActive()) {
                lock.lock();
                try {
                    if (isRunning) {
                        invokers.add(refereneceInvoker);
                        signalAvailableInvoker();
                    }
                } finally {
                    lock.unlock();
                }
            }
        });
    }

    public void destroy() {
        log.info("zookeeper directory destroy...");
        isRunning = false;
        //关闭共享EventLoopGroup
        eventLoopGroup.shutdownGracefully();
        //关闭注册中心连接,此时就不会再更新invokers列表,故不用加锁
        registry.destroy();
        //关闭所有当前连接
        lock.lock();
        try {
            if (invokers != null && invokers.size() > 0) {
                for (ReferenceInvoker invoker : invokers) {
                    invoker.shutdown();
                }
                invokers.clear();
            }
        } finally {
            lock.unlock();
        }
        //释放所有等待idle invoker的线程
        this.signalAvailableInvoker();
    }

}
