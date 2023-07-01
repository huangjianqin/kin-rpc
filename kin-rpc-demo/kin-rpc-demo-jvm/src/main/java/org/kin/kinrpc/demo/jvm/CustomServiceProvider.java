package org.kin.kinrpc.demo.jvm;

import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.ExecutorConfig;
import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.demo.api.Constants;
import org.kin.kinrpc.demo.api.CustomService;
import org.kin.kinrpc.demo.api.CustomServiceImpl;
import org.kin.kinrpc.demo.api.LogInterceptor;

import java.io.IOException;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/1
 */
public class CustomServiceProvider {
    public static void main(String[] args) throws IOException {
        ServiceConfig<CustomService> serviceConfig = null;
        try {
            serviceConfig = ServiceConfig.create(CustomService.class, new CustomServiceImpl())
                    .jvm()
                    .serviceName(Constants.CUSTOM_SERVICE_NAME)
                    .app(ApplicationConfig.create("kinrpc-demo-jvm-provider"))
                    .servers()
                    .executor(ExecutorConfig.fix())
                    .weight(1)
                    .interceptor(new LogInterceptor())
                    .export();

            System.in.read();
        } finally {
            if (Objects.nonNull(serviceConfig)) {
                serviceConfig.unExport();
            }
        }
    }
}
