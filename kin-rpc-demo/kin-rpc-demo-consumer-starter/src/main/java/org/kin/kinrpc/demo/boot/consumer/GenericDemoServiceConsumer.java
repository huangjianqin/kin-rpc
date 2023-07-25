package org.kin.kinrpc.demo.boot.consumer;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.boot.KinRpcHandler;
import org.kin.kinrpc.boot.KinRpcReference;
import org.kin.kinrpc.constants.CacheConstants;
import org.kin.kinrpc.demo.api.RemoteServiceConsumer;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
//@Component
public class GenericDemoServiceConsumer extends RemoteServiceConsumer implements ApplicationRunner {
    @KinRpcReference(serviceName = "demo",
            generic = true,
            handlers = {@KinRpcHandler(name = "asyncFind"),
                    @KinRpcHandler(name = "delayRandom", sticky = true, retries = 2),
                    @KinRpcHandler(name = "delayRandom2", sticky = true, retries = 2,
                            rpcTimeout = 10_000, cache = "expiring",
                            attachments = {CacheConstants.EXPIRING_CACHE_TTL, "1500"}),
                    @KinRpcHandler(name = "asyncFind2", async = true),
            })
    private GenericService genericDemoService;

    @Override
    public void run(ApplicationArguments args) {
        invokeGenericDemoService(genericDemoService);
    }
}