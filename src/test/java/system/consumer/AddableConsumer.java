package system.consumer;

import org.kinrpc.config.ApplicationConfig;
import org.kinrpc.config.ReferenceConfig;
import org.kinrpc.config.ZookeeperRegistryConfig;
import system.service.Addable;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class AddableConsumer {
    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppName("AddableConsumer");

        ZookeeperRegistryConfig zookeeperRegistryConfig = new ZookeeperRegistryConfig();
        zookeeperRegistryConfig.setHost("127.0.0.1");

        final ReferenceConfig<Addable> referenceConfig = new ReferenceConfig<Addable>();
        referenceConfig.setApplicationConfig(applicationConfig);
        referenceConfig.setRegistryConfig(zookeeperRegistryConfig);
        referenceConfig.setInterfaceClass(Addable.class);

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                referenceConfig.disable();
            }
        }));

        Addable service = referenceConfig.get();

        System.out.println("结果" + service.add(1, 1));
    }
}
