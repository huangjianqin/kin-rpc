package org.kin.kinrpc.demo.kinrpc;

import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.api.Constants;
import org.kin.kinrpc.demo.api.DemoService;
import org.kin.kinrpc.demo.api.DemoServiceImpl;
import org.kin.kinrpc.demo.api.LogInterceptor;

import java.io.IOException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceApplication {
    public static void main(String[] args) throws IOException {
        ServiceConfig<DemoService> serviceConfig = null;
        try {
            serviceConfig = ServiceConfig.create(DemoService.class, new DemoServiceImpl())
                    .servers(ServerConfig.kinrpc(Integer.parseInt(args[0])))
                    .serviceName(Constants.DEMO_SERVICE_NAME)
                    .app(ApplicationConfig.create("kinrpc-demo-kinrpc-provider"))
                    .executor(ExecutorConfig.fix())
                    .weight(1)
                    .interceptor(new LogInterceptor(true))
                    .export();

            System.in.read();
        } finally {
            if (Objects.nonNull(serviceConfig)) {
                serviceConfig.unExport();
            }
        }
    }
}
