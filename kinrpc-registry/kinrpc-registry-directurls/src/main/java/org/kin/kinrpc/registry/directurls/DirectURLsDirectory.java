package org.kin.kinrpc.registry.directurls;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.registry.AbstractDirectory;
import org.kin.kinrpc.rpc.RPCReference;
import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvokerImpl;
import org.kin.kinrpc.rpc.serializer.SerializerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/6/11.
 * direct url, 直接根据给定的host port连接并调用服务
 */
public class DirectURLsDirectory extends AbstractDirectory {
    private static final Logger log = LoggerFactory.getLogger("registry");
    private List<AbstractReferenceInvoker> invokers;

    public DirectURLsDirectory(String serviceName, int connectTimeout, List<HostAndPort> hostAndPorts, SerializerType serializerType) {
        super(serviceName, connectTimeout, serializerType);

        init(hostAndPorts);
    }

    private void init(List<HostAndPort> hostAndPorts) {
        invokers = new CopyOnWriteArrayList<>();
        for (HostAndPort hostAndPort : hostAndPorts) {
            connectServer(hostAndPort.getHost(), hostAndPort.getPort());
        }
    }

    /**
     * 创建新的ReferenceInvoker,连接Service Server
     */
    private void connectServer(String host, int port) {
        //创建连接
        RPCReference rpcReference = new RPCReference(new InetSocketAddress(host, port), serializerType.newInstance());
        AbstractReferenceInvoker refereneceInvoker = new ReferenceInvokerImpl(serviceName, rpcReference);
        //真正启动连接
        refereneceInvoker.init();

        if (refereneceInvoker.isActive()) {
            invokers.add(refereneceInvoker);
        }
    }

    @Override
    public List<AbstractReferenceInvoker> list() {
        Map<Boolean, List<AbstractReferenceInvoker>> map = this.invokers.stream().collect(Collectors.groupingBy(AbstractReferenceInvoker::isActive));
        if(map.containsKey(Boolean.FALSE)){
            invokers.removeAll(map.get(Boolean.FALSE));
            for (AbstractReferenceInvoker invoker : map.get(Boolean.FALSE)) {
                invoker.shutdown();
            }
        }

        return map.getOrDefault(Boolean.TRUE, Collections.emptyList());
    }

    @Override
    public void destroy() {
        for (AbstractReferenceInvoker invoker : invokers) {
            invoker.shutdown();
        }
        invokers.clear();
    }
}
