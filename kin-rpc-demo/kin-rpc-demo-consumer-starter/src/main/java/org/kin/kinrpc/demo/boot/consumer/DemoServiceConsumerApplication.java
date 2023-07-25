package org.kin.kinrpc.demo.boot.consumer;

import org.kin.kinrpc.ReferenceContext;
import org.kin.kinrpc.boot.EnableKinRpc;
import org.kin.kinrpc.boot.KinRpcHandler;
import org.kin.kinrpc.boot.KinRpcReference;
import org.kin.kinrpc.boot.KinRpcReferenceBean;
import org.kin.kinrpc.constants.CacheConstants;
import org.kin.kinrpc.demo.api.DemoService;
import org.kin.kinrpc.demo.api.LogFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
@SpringBootApplication
@EnableKinRpc
@EnableWebMvc
public class DemoServiceConsumerApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoServiceConsumerApplication.class);
        ReferenceContext.SCHEDULER.schedule(System::gc, 5, TimeUnit.SECONDS);
    }

    @Bean
    public LogFilter logFilter() {
        return new LogFilter(false);
    }

    @KinRpcReference(serviceName = "demo",
            handlers = {@KinRpcHandler(name = "asyncFind"),
                    @KinRpcHandler(name = "delayRandom", sticky = true, retries = 2),
                    @KinRpcHandler(name = "delayRandom2", sticky = true, retries = 2,
                            rpcTimeout = 10_000, cache = "expiring",
                            attachments = {CacheConstants.EXPIRING_CACHE_TTL, "1500"}),
                    @KinRpcHandler(name = "asyncFind2", async = true),
            })
    @Bean
    public KinRpcReferenceBean<DemoService> demoServiceFactoryBean() {
        return new KinRpcReferenceBean<>(DemoService.class);
    }
}
