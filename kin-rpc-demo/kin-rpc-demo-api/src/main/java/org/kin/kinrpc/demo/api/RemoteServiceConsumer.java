package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.config.*;

/**
 * @author huangjianqin
 * @date 2023/7/5
 */
public class RemoteServiceConsumer extends ServiceConsumer {
    public static void invoke(String appNamePrefix, String protocol) {
//        Map<String, String> metadata = new HashMap<>();
//        metadata.put(ServiceMetadataConstants.SCHEMA_KEY, protocol);
//        metadata.put(ServiceMetadataConstants.SERIALIZATION_KEY, SerializationType.JSON.getName());
//        metadata.put(ServiceMetadataConstants.WEIGHT_KEY, "1");
//        DefaultServiceInstance instance1 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
//                "127.0.0.1", Constants.SERVER_PORT1, metadata);
//        DefaultServiceInstance instance2 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
//                "127.0.0.1", Constants.SERVER_PORT2, metadata);
//        DefaultServiceInstance instance3 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
//                "127.0.0.1", Constants.SERVER_PORT3, metadata);
//        DefaultServiceInstance instance4 = new DefaultServiceInstance(GsvUtils.service("kinrpc", Constants.DEMO_SERVICE_NAME, "0.1.0.0"),
//                "127.0.0.1", Constants.SERVER_PORT4, metadata);

//        String address = String.join(RegistryConfig.ADDRESS_SEPARATOR,
//                RegistryHelper.toUrlStr(instance1),
//                RegistryHelper.toUrlStr(instance2),
//                RegistryHelper.toUrlStr(instance3),
//                RegistryHelper.toUrlStr(instance4)
//        );

        String address = String.join(RegistryConfig.ADDRESS_SEPARATOR,
                "kinrpc://127.0.0.1:13000/kinrpc:demo:0.1.0.0?schema=kinrpc&serialization=json&weight=1"
//                "kinrpc://127.0.0.1:13100/kinrpc:demo:0.1.0.0?schema=kinrpc&serialization=json&weight=2",
//                "kinrpc://127.0.0.1:13200/kinrpc:demo:0.1.0.0?schema=kinrpc&serialization=json&weight=3"
//                "kinrpc://127.0.0.1:13300/kinrpc/demo:0.1.0.0?schema=kinrpc&serialization=json&weight=4"
        );
        RegistryConfig registryConfig = RegistryConfig.direct(address);
        ReferenceConfig<DemoService> referenceConfig = ReferenceConfig.create(DemoService.class)
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create(appNamePrefix + "-consumer"))
                .cluster(ClusterType.FAILOVER)
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .method(MethodConfig.create("delayRandom").sticky().retries(2))
                .method(MethodConfig.create("asyncFind2").async())
                .filter(new LogFilter(false));

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .generic()
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create(appNamePrefix + "-generic-consumer"))
                .cluster(ClusterType.FAILOVER)
                .method(MethodConfig.create("asyncFind").timeout(4000))
                .method(MethodConfig.create("delayRandom").sticky().retries(2))
                .method(MethodConfig.create("asyncFind2").async())
                .filter(new LogFilter(false));

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

        try {
            System.in.read();
            Thread.sleep(2_000);
            System.out.println("force application exit>>>");
            System.exit(0);
        } catch (Exception e) {
            //do nothing
        }
    }
}
