package org.kin.kinrpc.demo.jvm;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.demo.api.*;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceApplication extends ServiceConsumer {
    private static ServiceConfig<DemoService> serviceConfig;

    public static void main(String[] args) throws InterruptedException {
        try {
            export();

            Thread.sleep(300);

            consume();
        } finally {
            if (Objects.nonNull(serviceConfig)) {
                serviceConfig.unExport();
            }
        }
    }

    private static void export() {
        serviceConfig = ServiceConfig.create(DemoService.class, new DemoServiceImpl())
                .jvm()
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-provider"))
                .executor(ExecutorConfig.fix())
                .weight(1)
                .interceptor(new LogInterceptor(true))
                .export();
    }

    private static void consume() throws InterruptedException {
        ReferenceConfig<DemoService> referenceConfig = ReferenceConfig.create(DemoService.class)
                .jvm()
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor(false));
        try {
            DemoService demoService = referenceConfig.refer();
            invokeDemoService(demoService);
        } finally {
            referenceConfig.unRefer();
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .jvm()
                .generic()
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-generic-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor(false));
        try {
            GenericService genericDemoService = genericReferenceConfig.refer();
            invokeGenericDemoService(genericDemoService);
        } finally {
            genericReferenceConfig.unRefer();
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }
}
