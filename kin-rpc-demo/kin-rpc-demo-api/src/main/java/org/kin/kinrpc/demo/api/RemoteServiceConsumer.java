package org.kin.kinrpc.demo.api;

import org.kin.kinrpc.GenericService;
import org.kin.kinrpc.bootstrap.KinRpcBootstrap;
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
//        metadata.put(ServiceMetadataConstants.TOKEN_KEY, "123456");
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
                protocol + "://127.0.0.1:13000/kinrpc:demo:0.1.0.0?serialization=json&weight=1&token=123456"
//                protocol+ "://127.0.0.1:13100/kinrpc:demo:0.1.0.0?serialization=json&weight=2&token=123456",
//                protocol+ "://127.0.0.1:13200/kinrpc:demo:0.1.0.0?serialization=json&weight=3&token=123456"
//                protocol+ "://127.0.0.1:13300/kinrpc/demo:0.1.0.0?serialization=json&weight=4&token=123456"
        );
        RegistryConfig registryConfig = RegistryConfig.direct(address);
        ReferenceConfig<DemoService> referenceConfig = ReferenceConfig.create(DemoService.class)
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create(appNamePrefix + "-consumer"))
                .cluster(ClusterType.FAILOVER)
                .handler(MethodConfig.create("asyncFind").timeout(4000))
                .handler(MethodConfig.create("delayRandom").sticky().retries(2))
                .handler(MethodConfig.create("asyncFind2").async())
                .filter(new LogFilter(false));

        ReferenceConfig<GenericService> genericReferenceConfig = ReferenceConfig.create(GenericService.class)
                .generic()
                .registries(registryConfig)
                .serviceName(Constants.DEMO_SERVICE_NAME)
                .app(ApplicationConfig.create(appNamePrefix + "-generic-consumer"))
                .cluster(ClusterType.FAILOVER)
                .handler(MethodConfig.create("asyncFind").timeout(4000))
                .handler(MethodConfig.create("delayRandom").sticky().retries(2))
                .handler(MethodConfig.create("asyncFind2").async())
                .filter(new LogFilter(false));

        try {
            DemoService demoService = referenceConfig.refer();
            System.in.read();
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
            System.out.println("force application exit>>>");
            System.exit(0);
        } catch (Exception e) {
            //do nothing
        }
    }

    /**
     * 使用{@link org.kin.kinrpc.bootstrap.KinRpcBootstrap}获取服务引用
     */
    public static void invoke2(String appNamePrefix, String protocol) {
        String address = String.join(RegistryConfig.ADDRESS_SEPARATOR,
                protocol + "://127.0.0.1:13000/kinrpc:demo:0.1.0.0?serialization=json&weight=1&token=123456"
//                protocol+ "://127.0.0.1:13100/kinrpc:demo:0.1.0.0?serialization=json&weight=2&token=123456",
//                protocol+ "://127.0.0.1:13200/kinrpc:demo:0.1.0.0?serialization=json&weight=3&token=123456"
//                protocol+ "://127.0.0.1:13300/kinrpc/demo:0.1.0.0?serialization=json&weight=4&token=123456"
        );

        KinRpcBootstrap.instance()
                .registries(RegistryConfig.direct(address))
                .app(ApplicationConfig.create(appNamePrefix + "-consumer"))
                .consumer(ConsumerConfig.create()
                        .cluster(ClusterType.FAILOVER)
                        .rpcTimeout(2000)
                        .retries(4)
                        .filter(new LogFilter(false)))
                .reference(ReferenceConfig.create(DemoService.class)
                        .serviceName(Constants.DEMO_SERVICE_NAME)
                        .handler(MethodConfig.create("asyncFind").timeout(3000))
                        .handler(MethodConfig.create("delayRandom").sticky().retries(2))
                        .handler(MethodConfig.create("asyncFind2").async()))
                .asyncExportRefer()
                .start();

        try {
            DemoService demoService = KinRpcBootstrap.instance().reference(DemoService.class);
            System.in.read();
            invokeDemoService(demoService);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        } finally {
            KinRpcBootstrap.instance().destroy();
        }

        try {
            System.in.read();
            System.out.println("force application exit>>>");
            System.exit(0);
        } catch (Exception e) {
            //do nothing
        }
    }
}
