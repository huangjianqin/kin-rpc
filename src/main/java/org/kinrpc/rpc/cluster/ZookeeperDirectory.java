package org.kinrpc.rpc.cluster;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.kinrpc.common.Constants;

import org.kinrpc.registry.zookeeper.ZookeeperRegistry;
import org.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kinrpc.rpc.invoker.SimpleReferenceInvoker;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by 健勤 on 2017/2/13.
 */
public class ZookeeperDirectory implements Directory{
    private static final Logger log = Logger.getLogger(ZookeeperDirectory.class);

    private ZookeeperRegistry registry;
    private Class<?> interfaceClass;

    //用于启动invoker连接的线程池
    private ThreadPoolExecutor threads = new ThreadPoolExecutor(2, 16, 600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private CopyOnWriteArrayList<ReferenceInvoker> invokers;
    //用于在没有invoker可用时阻塞
    private Lock lock = new ReentrantLock();
    private Condition hasInvoker = lock.newCondition();
    private boolean isRunning = true;
    //所有的消费者共用一个EventLoopGroup
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup();

    public ZookeeperDirectory(ZookeeperRegistry registry, Class<?> interfaceClass) {
        this.registry = registry;
        this.interfaceClass = interfaceClass;
    }

    /**
     * 获取当前可用的所有ReferenceInvoker
     * @return
     */
    public List<ReferenceInvoker> list(){
        log.info("zookeeper directory listing");
        //第一次调用
        if(invokers == null){
            invokers = new CopyOnWriteArrayList<ReferenceInvoker>();
            discover();
            if(invokers.size() == 0){
                try {
                    waitingForAvailableInvoker();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }


        List<ReferenceInvoker> shallowClonedInvokers = (List<ReferenceInvoker>)this.invokers.clone();
        int size = shallowClonedInvokers.size();
        while(isRunning && size <= 0){
            try {
                boolean hasAvailableInvoker = waitingForAvailableInvoker();
                if(hasAvailableInvoker){
                    shallowClonedInvokers = (List<ReferenceInvoker>)this.invokers.clone();
                    size = shallowClonedInvokers.size();
                }
            } catch (InterruptedException e) {
                log.info("waiting for available invoker is interrupted");
                e.printStackTrace();
            }
        }

        return shallowClonedInvokers;
    }

    /**
     * 如果可用的ReferenceInvoker,释放等待可用ReferenceInvoker的线程
     */
    private void signalAvailableInvoker() {
        lock.lock();
        try {
            hasInvoker.signalAll();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 若没有可用ReferenceInvoker,等待一段时间
     * @return
     * @throws InterruptedException
     */
    private boolean waitingForAvailableInvoker() throws InterruptedException {
        lock.lock();
        try {
            return hasInvoker.await(5000, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 发现服务,发现可用服务的address
     */
    private void discover(){
        log.info("consumer discover service...");
        ZooKeeper zooKeeper = registry.getConnection();
        try {
            //获取注册服务的所有address
            List<String> znodeList = zooKeeper.getChildren(Constants.REGISTRY_ROOT + Constants.REGISTRY_PAHT_SEPARATOR + getServiceName(), new Watcher() {
                public void process(WatchedEvent watchedEvent) {
                    if(watchedEvent.getType() == Event.EventType.NodeChildrenChanged){
                        log.info("service '" + getServiceName() + "' Server node changed");
                        discover();
                    }
                }
            });

            //更新当前的invoker
            updateCurrentInvoker(znodeList);
        } catch (KeeperException e) {
            e.printStackTrace();
            //如果不存在该节点,则表明服务取消注册了
            if(e.code().equals(KeeperException.Code.NONODE)){
                log.info("service '" + getServiceName() + "' unregisted");
                updateCurrentInvoker(null);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void updateCurrentInvoker(List<String> addresses){
        log.info("update current invoker...");
        String addressesStr = addresses.get(0);
        for(int i = 1; i < addresses.size(); i++){
            addressesStr += " , " + addresses.get(i);
        }
        log.info("current address: " + addressesStr);

        List<ReferenceInvoker> invalidInvoker = new ArrayList<ReferenceInvoker>();
        if(addresses != null && addresses.size() > 0){
            //该循环处理完后,addresses里面
            for(ReferenceInvoker invoker: invokers){
                //不包含该连接,而是连接变为无用,shutdown
                if(!addresses.contains(invoker.getAddress())){
                   invalidInvoker.add(invoker);
                }else{
                    //连接仍然有效,故addresses仍然有该连接的host:port
                    addresses.remove(invoker.getAddress());
                }
            }
            //清除掉invokers中的无效Invoker
            invokers.removeAll(invalidInvoker);

            //构建新的ReferenceInvoker
            List<ReferenceInvoker> newInvoker = new ArrayList<ReferenceInvoker>();
            for(String address: addresses){
                //address有效,创建ReferenceInvoker
//                if(address.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}:\\d{1,5}")){
//                    String host = address.split(":")[0];
//                    int port = Integer.valueOf(address.split(":")[1]);
//                    connectServer(host, port);
//                }
//                else{
//                    //address格式错误,不创建ReferenceInvoker
//                    log.error("service address('" + address + "') format error!!!");
//                }
                //测试用
                String host = address.split(":")[0];
                int port = Integer.valueOf(address.split(":")[1]);
                connectServer(host, port);
            }

        }
        else{
            //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
            invalidInvoker.addAll(this.invokers);
            this.invokers.clear();
        }

        //移除所有无效的Service Server
        removeInvalidServer(invalidInvoker);
    }

    /**
     * shutdown invoker, 包括底层的连接
     * @param invokers
     */
    private void removeInvalidServer(List<ReferenceInvoker> invokers){
        for(ReferenceInvoker invoker: invokers){
            invoker.shutdown();
        }
    }

    /**
     * 创建新的ReferenceInvoker,连接Service Server
     * @param host
     * @param port
     */
    private void connectServer(final String host, final int port){
        threads.submit(new Runnable() {
            public void run() {
                ReferenceInvoker refereneceInvoker = new SimpleReferenceInvoker(interfaceClass);
                refereneceInvoker.init(host, port, eventLoopGroup);
                addInvoker(refereneceInvoker);
            }
        });
    }

    /**
     * 添加新ReferenceInvoker,并释放等待可用ReferenceInvoker的线程
     * @param invoker
     */
    private void addInvoker(ReferenceInvoker invoker){
        invokers.add(invoker);
        signalAvailableInvoker();
    }

    public String getServiceName(){
        return interfaceClass.getName();
    }

    public void destroy(){
        log.info("zookeeper directory destroy...");
        isRunning = false;
        //释放所有等待idle invoker的线程
        this.signalAvailableInvoker();
        //关闭注册中心连接,此时就不会再更新invokers列表,故不用加锁
        registry.destroy();
        //关闭所有当前连接
        for(ReferenceInvoker invoker: invokers){
            invoker.shutdown();
        }
        //关闭共享EventLoopGroup
        eventLoopGroup.shutdownGracefully();
        //关闭线程池
        this.threads.shutdown();
    }
}
