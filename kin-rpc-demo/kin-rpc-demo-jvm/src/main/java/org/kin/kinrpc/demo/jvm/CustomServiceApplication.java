package org.kin.kinrpc.demo.jvm;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.demo.api.*;

import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class CustomServiceApplication extends CustomServiceConsumerBase {
    private static ServiceConfig<CustomService> serviceConfig;

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
        serviceConfig = ServiceConfig.create(CustomService.class, new CustomServiceImpl())
                .jvm()
                .serviceName(Constants.CUSTOM_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-provider"))
                .executor(ExecutorConfig.fix())
                .weight(1)
                .interceptor(new LogInterceptor(true))
                .export();
    }

    private static void consume() throws InterruptedException {
        ReferenceConfig<CustomService> referenceConfig = ReferenceConfig.create(CustomService.class)
                .jvm()
                .serviceName(Constants.CUSTOM_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor(false));
        try {
            CustomService customService = referenceConfig.refer();
            invokeCustomService(customService);
        } finally {
            referenceConfig.unRefer();
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .jvm()
                .generic()
                .serviceName(Constants.CUSTOM_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-generic-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor(false));
        try {
            GenericService genericCustomService = genericReferenceConfig.refer();
            invokeGenericCustomService(genericCustomService);
        } finally {
            genericReferenceConfig.unRefer();
        }
        System.out.println("------------------------------------------------------------------------------------------------------------------------------------------------------------");
    }
}
