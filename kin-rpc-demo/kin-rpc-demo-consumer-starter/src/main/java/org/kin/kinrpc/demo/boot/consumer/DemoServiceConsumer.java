package org.kin.kinrpc.demo.boot.consumer;

import org.kin.kinrpc.demo.api.DemoService;
import org.kin.kinrpc.demo.api.RemoteServiceConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
@Component
public class DemoServiceConsumer extends RemoteServiceConsumer implements ApplicationRunner {
    @Autowired
    private DemoService demoService;
//    @KinRpcReference(serviceName = "demo",
//            generic = true,
//            handlers = {@KinRpcHandler(name = "asyncFind"),
//                    @KinRpcHandler(name = "delayRandom", sticky = true, retries = 2),
//                    @KinRpcHandler(name = "asyncFind2", async = true),
//            })
//    private GenericService genericDemoService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        invokeDemoService(demoService);

//        invokeGenericDemoService(genericDemoService);
    }
}
