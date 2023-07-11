package org.kin.kinrpc.demo.boot.provider;

import org.kin.kinrpc.boot.KinRpcService;
import org.kin.kinrpc.demo.api.DemoService;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
@KinRpcService(interfaceClass = DemoService.class, serviceName = "demo")
public class DemoServiceImpl extends org.kin.kinrpc.demo.api.DemoServiceImpl {
}
