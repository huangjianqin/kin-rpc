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
                    .filter(new LogFilter(true))
                    .delay(3000)
                    .token("123456")
                    .export();

            System.in.read();

            Thread.sleep(2_000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (Objects.nonNull(serviceConfig)) {
                serviceConfig.unExport();
            }
        }
    }
}
