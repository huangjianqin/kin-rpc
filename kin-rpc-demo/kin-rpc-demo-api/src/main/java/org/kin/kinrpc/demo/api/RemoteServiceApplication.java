package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
import org.kin.kinrpc.config.*;

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

    /**
     * 使用{@link KinRpcBootstrap}发布服务
     */
    public static void export2(ServerConfig... servers) {
        try {
            KinRpcBootstrap.instance()
                    .app(ApplicationConfig.create("kinrpc-demo-kinrpc-provider"))
                    .servers(servers)
                    .executor(ExecutorConfig.fix().id(Constants.SERVICE_EXECUTOR_CONFIG_ID))
                    .provider(ProviderConfig.create()
                            .executor(ExecutorConfig.fromId(Constants.SERVICE_EXECUTOR_CONFIG_ID))
                            .weight(1)
                            .filter(new LogFilter(true))
                            .delay(3000)
                            .token("123456"))
                    .service(ServiceConfig.create(DemoService.class, new DemoServiceImpl())
                            .serviceName(Constants.DEMO_SERVICE_NAME)
                            .weight(2))
                    .asyncExportRefer()
                    .start();

            System.in.read();

            Thread.sleep(2_000);
            System.exit(0);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            KinRpcBootstrap.instance()
                    .destroy();
        }
    }
}
