package org.kin.kinrpc.registry.directurls;

import com.google.common.net.HostAndPort;
import org.kin.kinrpc.registry.AbstractDirectory;
import org.kin.kinrpc.rpc.RpcReference;
import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;
import org.kin.kinrpc.serializer.Serializers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangjianqin on 2019/6/11.
 * direct url, 直接根据给定的host port连接并调用服务
 */
public class DirectURLsDirectory extends AbstractDirectory {
    private static final Logger log = LoggerFactory.getLogger(DirectURLsDirectory.class);

    public DirectURLsDirectory(String serviceName, int connectTimeout, String serializerType, boolean compression) {
        super(serviceName, connectTimeout, serializerType, compression);
    }

    @Override
    protected List<ReferenceInvoker> doDiscover(List<String> addresses, List<ReferenceInvoker> originInvokers) {
        List<ReferenceInvoker> invokers = new ArrayList<>();
        for (String address : addresses) {
            HostAndPort hostAndPort = HostAndPort.fromString(address);

            //创建新的ReferenceInvoker,连接Service Server
            RpcReference rpcReference = new RpcReference(serviceName, new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()),
                    Serializers.getSerializer(serializerType), connectTimeout, compression);
            ReferenceInvoker refereneceInvoker = new ReferenceInvoker(serviceName, rpcReference);
            //真正启动连接
            refereneceInvoker.init();
            invokers.add(refereneceInvoker);
        }
        return invokers;
    }

    @Override
    protected void doDestroy() {
    }
}
