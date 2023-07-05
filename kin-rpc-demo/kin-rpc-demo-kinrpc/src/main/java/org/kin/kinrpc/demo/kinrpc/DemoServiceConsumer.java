package org.kin.kinrpc.demo.kinrpc;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.config.*;
import org.kin.kinrpc.demo.api.Constants;
import org.kin.kinrpc.demo.api.DemoService;
import org.kin.kinrpc.demo.api.DemoServiceConsumerBase;
import org.kin.kinrpc.demo.api.LogInterceptor;
import org.kin.kinrpc.registry.DefaultServiceInstance;
import org.kin.kinrpc.registry.RegistryHelper;
import org.kin.kinrpc.registry.ServiceMetadataConstants;
import org.kin.kinrpc.utils.GsvUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceConsumer extends DemoServiceConsumerBase {
    public static void main(String[] args) throws IOException, InterruptedException {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(ServiceMetadataConstants.SCHEMA_KEY, ProtocolType.KINRPC.getName());
        metadata.put(ServiceMetadataConstants.SERIALIZATION_KEY, SerializationType.JSON.getName());
        metadata.put(ServiceMetadataConstants.WEIGHT_KEY, "1");
        DefaultServiceInstance instance1 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
                "127.0.0.1", Constants.SERVER_PORT1, metadata);
        DefaultServiceInstance instance2 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
                "127.0.0.1", Constants.SERVER_PORT2, metadata);
        DefaultServiceInstance instance3 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
                "127.0.0.1", Constants.SERVER_PORT3, metadata);
//        DefaultServiceInstance instance4 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
//                "127.0.0.1", Constants.SERVER_PORT4, metadata);

        String address = String.join(RegistryConfig.ADDRESS_SEPARATOR,
                RegistryHelper.toUrlStr(instance1),
                RegistryHelper.toUrlStr(instance2),
                RegistryHelper.toUrlStr(instance3)
//                RegistryHelper.toUrlStr(instance4)
        );
        RegistryConfig registryConfig = RegistryConfig.direct(address);
        ReferenceConfig<DemoService> referenceConfig = ReferenceConfig.create(DemoService.class)
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-kinrpc-consumer"))
                .cluster(ClusterType.FAILOVER)
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .method(MethodConfig.create("delayRandom").sticky().retries(2))
                .interceptor(new LogInterceptor(false));

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .generic()
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create("kinrpc-demo-kinrpc-generic-consumer"))
                .cluster(ClusterType.FAILOVER)
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .method(MethodConfig.create("delayRandom").sticky().retries(2))
                .interceptor(new LogInterceptor(false));

        try {
            DemoService demoService = referenceConfig.refer();
            // TODO: 2023/7/4 需要等待directory建立invoker
            System.in.read();
            Thread.sleep(2_000);
            invokeDemoService(demoService);

            System.out.println("------------------------------------------------------------------------------------------------------------------------------------");
            System.in.read();
            GenericService genericDemoService = genericReferenceConfig.refer();
            invokeGenericDemoService(genericDemoService);

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            referenceConfig.unRefer();
            genericReferenceConfig.unRefer();
        }

        System.in.read();
        Thread.sleep(2_000);
        System.out.println("force application exit>>>");
        System.exit(0);
    }
}