package org.kin.kinrpc.registry.zookeeper;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.registry.AbstractDirectory;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.kin.kinrpc.rpc.serializer.Serializers;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by 健勤 on 2017/2/13.
 * 以zookeeper为注册中心, 实时监听服务状态变化, 并更新可调用服务invoker
 * <p>
 * 无效invoker由zookeeper注册中心控制, 所以可能会存在list有无效invoker(zookeeper没有及时更新到)
 */
public class ZookeeperDirectory extends AbstractDirectory {
    public ZookeeperDirectory(String serviceName, int connectTimeout, String serializerType) {
        super(serviceName, connectTimeout, serializerType);
    }

    /**
     * 获取当前可用invokers
     */
    @Override
    public List<ReferenceInvoker> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            return super.invokers.stream().filter(ReferenceInvoker::isActive).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    /**
     * 发现服务,发现可用服务的address
     */
    @Override
    protected List<ReferenceInvoker> doDiscover(List<String> addresses) {
        if (isStopped) {
            return Collections.emptyList();
        }

        StringBuilder sb = new StringBuilder();
        List<HostAndPort> hostAndPorts = new ArrayList<>();
        if (addresses != null && addresses.size() > 0) {
            for (String address : addresses) {
                HostAndPort hostAndPort = HostAndPort.fromString(address);
                hostAndPorts.add(hostAndPort);

                sb.append(hostAndPort.toString() + ", ");
            }
        }
        log.info("discover service '{}'..." + System.lineSeparator() + "current service address: " + sb.toString(), getServiceName());

        List<ReferenceInvoker> newInvokerList = new ArrayList<>(invokers);
        List<ReferenceInvoker> invalidInvokers = new ArrayList<>();
        if (hostAndPorts.size() > 0) {
            //该循环处理完后,addresses里面的都是新的
            for (ReferenceInvoker invoker : newInvokerList) {
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
            newInvokerList.removeAll(invalidInvokers);
        } else {
            //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
            invalidInvokers.addAll(newInvokerList);
            newInvokerList.clear();
        }

        //new ReferenceInvokers
        for (HostAndPort hostAndPort : hostAndPorts) {
            //address有效,创建新的ReferenceInvoker,连接Service Server
            RPCReference rpcReference = new RPCReference(
                    new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()), Serializers.getSerializer(serializerType), connectTimeout);
            ReferenceInvoker refereneceInvoker = new ReferenceInvoker(serviceName, rpcReference);
            //真正启动连接
            refereneceInvoker.init();

            newInvokerList.add(refereneceInvoker);
        }

        //remove invalid ReferenceInvokers
        for (ReferenceInvoker invoker : invalidInvokers) {
            invoker.shutdown();
        }

        log.info("discover service '{}' finished", getServiceName());

        return newInvokerList;
    }

    @Override
    protected void doDestroy() {
        if (!isStopped) {
            isStopped = true;
            //关闭所有当前连接
            if (invokers != null && invokers.size() > 0) {
                for (ReferenceInvoker invoker : invokers) {
                    invoker.shutdown();
                }
                invokers.clear();
                invokers = null;
            }
            log.info("zookeeper directory destroyed");
        }
    }

}
