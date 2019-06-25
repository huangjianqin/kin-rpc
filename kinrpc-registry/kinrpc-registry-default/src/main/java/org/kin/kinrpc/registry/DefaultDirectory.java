package org.kin.kinrpc.registry;

import com.google.common.net.HostAndPort;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.kinrpc.rpc.domain.RPCReference;
import org.kin.kinrpc.rpc.invoker.ReferenceInvoker;
import org.kin.kinrpc.rpc.invoker.impl.JavaReferenceInvoker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by huangjianqin on 2019/6/11.
 * direct url, 直接根据给定的host port连接并调用服务
 */
public class DefaultDirectory extends AbstractDirectory {
    private static final Logger log = LoggerFactory.getLogger("registry");
    private List<ReferenceInvoker> invokers;

    public DefaultDirectory(String serviceName, int connectTimeout, List<HostAndPort> hostAndPorts) {
        super(serviceName, connectTimeout);

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
        ThreadManager.DEFAULT.submit(() -> {
            //创建连接
            RPCReference rpcReference = new RPCReference(new InetSocketAddress(host, port));
            ReferenceInvoker refereneceInvoker = new JavaReferenceInvoker(serviceName, rpcReference);
            //真正启动连接
            refereneceInvoker.init();

            if (refereneceInvoker.isActive()) {
                invokers.add(refereneceInvoker);
            }
        });
    }

    @Override
    public List<ReferenceInvoker> list() {
        return new ArrayList<>(this.invokers);
    }

    @Override
    public void destroy() {
        for (ReferenceInvoker invoker : invokers) {
            invoker.shutdown();
        }
        invokers.clear();
    }
}
