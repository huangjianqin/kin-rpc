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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by huangjianqin on 2019/6/11.
 * direct url, 直接根据给定的host port连接并调用服务
 */
public class DirectURLsDirectory extends AbstractDirectory {
    private static final Logger log = LoggerFactory.getLogger(DirectURLsDirectory.class);

    public DirectURLsDirectory(String serviceName, int connectTimeout, SerializerType serializerType) {
        super(serviceName, connectTimeout, serializerType);
    }

    @Override
    public List<AbstractReferenceInvoker> list() {
        if(!isStopped){
            return invokers.stream().filter(AbstractReferenceInvoker::isActive).collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    @Override
    protected void doDiscover(List<String> addresses) {
        if(!isStopped){
            List<AbstractReferenceInvoker> invokers = new ArrayList<>();
            for (String address: addresses) {
                HostAndPort hostAndPort = HostAndPort.fromString(address);

                //创建新的ReferenceInvoker,连接Service Server
                RPCReference rpcReference = new RPCReference(new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()),
                        serializerType.newInstance(), connectTimeout);
                AbstractReferenceInvoker refereneceInvoker = new ReferenceInvokerImpl(serviceName, rpcReference);
                //真正启动连接
                refereneceInvoker.init();
                invokers.add(refereneceInvoker);
            }
            super.invokers = invokers;
        }
    }

    @Override
    protected void doDestroy() {
        if(!isStopped){
            isStopped = true;
            for (AbstractReferenceInvoker invoker : invokers) {
                invoker.shutdown();
            }
            invokers.clear();
            invokers = null;
        }
    }
}
