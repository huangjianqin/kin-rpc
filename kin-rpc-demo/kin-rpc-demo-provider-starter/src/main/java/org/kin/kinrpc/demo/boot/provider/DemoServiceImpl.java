package org.kin.kinrpc.demo.boot.provider;

import org.kin.kinrpc.boot.KinRpcService;
import org.kin.kinrpc.demo.api.DemoService;
import org.springframework.beans.factory.annotation.Value;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
@KinRpcService(interfaceClass = DemoService.class, serviceName = "demo")
public class DemoServiceImpl extends org.kin.kinrpc.demo.api.DemoServiceImpl {
    @Value("${kinrpc.server.port:-1}")
    private int rpcPort;

    @Override
    protected int getRpcPort() {
        return rpcPort;
    }
}
