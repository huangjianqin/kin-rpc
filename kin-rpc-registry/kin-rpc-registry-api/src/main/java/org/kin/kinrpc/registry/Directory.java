package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;
import org.kin.framework.utils.CollectionUtils;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.transport.RpcReference;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.transport.netty.CompressionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author huangjianqin
 * @date 2019/6/11
 */
public class Directory {
    private static final Logger log = LoggerFactory.getLogger(Directory.class);

    protected final String serviceName;
    protected final int connectTimeout;
    protected final int serializerType;
    protected final CompressionType compressionType;

    /** 所有directory的discover和destroy操作都是单线程操作, 利用copy-on-write思想更新可用invokers, 提高list效率 */
    private volatile List<ReferenceInvoker> invokers = Collections.emptyList();
    private volatile boolean isStopped;

    public Directory(String serviceName, int connectTimeout, int serializerType, CompressionType compressionType) {
        this.serviceName = serviceName;
        this.connectTimeout = connectTimeout;
        this.serializerType = serializerType;
        this.compressionType = compressionType;
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
    public List<ReferenceInvoker> list() {
        //Directory关闭中调用该方法会返回一个size=0的列表
        if (!isStopped) {
            return getActiveReferenceInvoker();
        }
        return Collections.emptyList();
    }

    /**
     * 发现可用服务的address, 并构建reference invoker
     */
    public void discover(List<String> addresses) {
        if (!isStopped) {
            ArrayList<ReferenceInvoker> originInvokers = new ArrayList<>(invokers);

            StringBuilder sb = new StringBuilder();
            List<HostAndPort> hostAndPorts = new ArrayList<>();
            if (addresses != null && addresses.size() > 0) {
                for (String address : addresses) {
                    HostAndPort hostAndPort = HostAndPort.fromString(address);
                    hostAndPorts.add(hostAndPort);

                    sb.append(hostAndPort.toString()).append(", ");
                }
            }
            log.info("discover service '{}'..."
                    .concat("current service address: ")
                    .concat(sb.toString()), getServiceName());

            List<ReferenceInvoker> validInvokers = new ArrayList<>(hostAndPorts.size());
            List<ReferenceInvoker> invalidInvokers = new ArrayList<>(originInvokers.size());
            if (CollectionUtils.isNonEmpty(hostAndPorts)) {
                for (ReferenceInvoker originInvoker : originInvokers) {
                    HostAndPort address = originInvoker.getAddress();
                    if (!hostAndPorts.contains(address)) {
                        //无效invoker
                        invalidInvokers.add(originInvoker);
                    } else {
                        //invoker仍然有效
                        validInvokers.add(originInvoker);
                        hostAndPorts.remove(address);
                    }
                }
            } else {
                //如果服务取消注册或者没有子节点(注册了但没有启动完连接),关闭所有现有的invoker
                invalidInvokers.addAll(originInvokers);
            }

            //new ReferenceInvokers
            for (HostAndPort hostAndPort : hostAndPorts) {
                //创建新的ReferenceInvoker,连接Service Server
                RpcReference rpcReference = new RpcReference(serviceName, new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()),
                        Serializers.getSerializer(serializerType), connectTimeout, compressionType);
                ReferenceInvoker refereneceInvoker = new ReferenceInvoker(serviceName, rpcReference);
                //真正启动连接
                refereneceInvoker.init();
                validInvokers.add(refereneceInvoker);
            }

            //remove invalid ReferenceInvokers
            for (ReferenceInvoker invoker : invalidInvokers) {
                invoker.shutdown();
            }

            //update cache
            updateInvokers(validInvokers);

            log.info("discover service '{}' finished", getServiceName());
        }
    }

    public void destroy() {
        if (!isStopped) {
            isStopped = true;
            for (ReferenceInvoker invoker : invokers) {
                invoker.shutdown();
            }
            invokers = null;
            log.info("zookeeper directory destroyed");
        }
    }

    /**
     * @return 服务名
     */
    public String getServiceName() {
        return serviceName;
    }
}
