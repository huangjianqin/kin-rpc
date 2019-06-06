//package org.kin.kinrpc.test.provider;
//
//import org.kin.kinrpc.api.ApplicationConfig;
//import org.kin.kinrpc.api.ServerConfig;
//import org.kin.kinrpc.api.ServiceConfig;
//import org.kin.kinrpc.api.ZookeeperRegistryConfig;
//import org.kin.kinrpc.rpc.future.ServiceFuture;
//import org.kin.kinrpc.test.service.Adder;
//
///**
// * Created by 健勤 on 2017/2/16.
// */
//public class AddableProvider {
//    public static void main(String[] args) throws InterruptedException {
//        ApplicationConfig applicationConfig = new ApplicationConfig();
//        applicationConfig.setAppName("AddableProvider");
//
//        ZookeeperRegistryConfig zookeeperRegistryConfig = new ZookeeperRegistryConfig("127.0.0.1");
//
//        final ServerConfig serverConfig = new ServerConfig();
//        serverConfig.setThreadNum(4);
//
//        final ServiceConfig serviceConfig = new ServiceConfig(new Adder(), Object.class);
//        serviceConfig.setApplicationConfig(applicationConfig);
//        serviceConfig.setRegistryConfig(zookeeperRegistryConfig);
//        serviceConfig.setServerConfig(serverConfig);
//
//        ServiceFuture future = serviceConfig.export();
//
////        future.sync(60000L);
//        serviceConfig.disable();
//    }
//}
