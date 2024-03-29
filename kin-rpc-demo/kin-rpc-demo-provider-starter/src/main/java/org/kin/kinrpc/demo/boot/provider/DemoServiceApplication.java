package org.kin.kinrpc.demo.boot.provider;

import org.kin.kinrpc.ReferenceContext;
import org.kin.kinrpc.boot.EnableKinRpc;
import org.kin.kinrpc.demo.api.LogFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2023/7/11
 */
@SpringBootApplication
@EnableKinRpc
public class DemoServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DemoServiceApplication.class);
        ReferenceContext.SCHEDULER.schedule(System::gc, 5, TimeUnit.SECONDS);
    }

    @Bean
    public LogFilter logFilter() {
        return new LogFilter(true);
    }

//    @KinRpcService(interfaceClass = DemoService.class, serviceName = "demo")
//    public DemoService demoService(){
//        return new DemoServiceImpl();
//    }
}
