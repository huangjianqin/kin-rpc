package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.config.ServiceConfig;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/5
 */
public class RemoteServiceApplication {
    public static void export(ServerConfig... servers) {
        ServiceConfig<DemoService> serviceConfig = null;
        try {
            serviceConfig = ServiceConfig.create(DemoService.class, new DemoServiceImpl())
                    .servers(servers)
                    .serviceName(Constants.DEMO_SERVICE_NAME)
                    .app(ApplicationConfig.create("kinrpc-demo-kinrpc-provider"))
                    .executor(ExecutorConfig.fix())
                    .weight(1)
                    .interceptor(new LogInterceptor(true))
                    .export();

            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(serviceConfig)) {
                serviceConfig.unExport();
            }
        }
    }
}
