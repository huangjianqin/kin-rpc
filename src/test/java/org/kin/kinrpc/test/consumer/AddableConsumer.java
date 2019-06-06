//package org.kin.kinrpc.test.consumer;
//
//import org.kin.kinrpc.api.ApplicationConfig;
//import org.kin.kinrpc.api.ReferenceConfig;
//import org.kin.kinrpc.api.ZookeeperRegistryConfig;
//import org.kin.kinrpc.test.service.Addable;
//
///**
// * Created by 健勤 on 2017/2/16.
// */
//public class AddableConsumer {
//    public static void main(String[] args) {
//        ApplicationConfig applicationConfig = new ApplicationConfig();
//        applicationConfig.setAppName("AddableConsumer");
//
//        ZookeeperRegistryConfig zookeeperRegistryConfig = new ZookeeperRegistryConfig("127.0.0.1");
//
//        final ReferenceConfig<Addable> referenceConfig = new ReferenceConfig<Addable>(Addable.class);
//        referenceConfig.setApplicationConfig(applicationConfig);
//        referenceConfig.setRegistryConfig(zookeeperRegistryConfig);
//
//        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
//            public void run() {
//                referenceConfig.disable();
//            }
//        }));
//
//        Addable service = referenceConfig.get();
//
//        System.out.println("结果" + service.add(1, 1));
//    }
//}
