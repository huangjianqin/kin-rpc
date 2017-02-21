package system.provider;

import org.kinrpc.config.ApplicationConfig;
import org.kinrpc.config.ServerConfig;
import org.kinrpc.config.ServiceConfig;
import org.kinrpc.config.ZookeeperRegistryConfig;
import org.kinrpc.rpc.future.ServiceFuture;
import system.service.Addable;
import system.service.Adder;

/**
 * Created by 健勤 on 2017/2/16.
 */
public class AddableProvider {
    public static void main(String[] args) {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setAppName("AddableProvider");

        ZookeeperRegistryConfig zookeeperRegistryConfig = new ZookeeperRegistryConfig();
        zookeeperRegistryConfig.setHost("127.0.0.1");

        final ServerConfig serverConfig = new ServerConfig();
        serverConfig.setPort(16888);
        serverConfig.setThreadNum(4);

        final ServiceConfig serviceConfig = new ServiceConfig();
        serviceConfig.setApplicationConfig(applicationConfig);
        serviceConfig.setRegistryConfig(zookeeperRegistryConfig);
        serviceConfig.setServerConfig(serverConfig);
        serviceConfig.setInterfaceClass(Addable.class);
        serviceConfig.setRef(new Adder());

        ServiceFuture future = serviceConfig.export();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                serviceConfig.disable();
            }
        }));


        future.sync();
    }
}
