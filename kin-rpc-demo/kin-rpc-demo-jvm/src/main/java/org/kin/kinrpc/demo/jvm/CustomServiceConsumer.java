package org.kin.kinrpc.demo.jvm;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.config.ApplicationConfig;
import org.kin.kinrpc.config.MethodConfig;
import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.demo.api.Constants;
import org.kin.kinrpc.demo.api.CustomService;
import org.kin.kinrpc.demo.api.CustomServiceConsumerBase;
import org.kin.kinrpc.demo.api.LogInterceptor;

/**
 * @author huangjianqin
 * @date 2023/7/1
 */
public class CustomServiceConsumer extends CustomServiceConsumerBase {
    public static void main(String[] args) {
        ReferenceConfig<CustomService> referenceConfig = ReferenceConfig.create(CustomService.class)
                .jvm()
                .serviceName(Constants.CUSTOM_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor());
        try {
            CustomService customService = referenceConfig.refer();
            invokeCustomService(customService);
        } finally {
            referenceConfig.unRefer();
        }

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .jvm()
                .generic()
                .serviceName(Constants.CUSTOM_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-jvm-generic-consumer"))
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .interceptor(new LogInterceptor());
        try {
            GenericService genericCustomService = genericReferenceConfig.refer();
            invokeGenericCustomService(genericCustomService);
        } finally {
            genericReferenceConfig.unRefer();
        }
    }
}
