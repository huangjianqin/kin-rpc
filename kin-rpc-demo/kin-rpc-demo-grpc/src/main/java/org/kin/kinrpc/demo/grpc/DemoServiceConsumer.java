package org.kin.kinrpc.demo.grpc;

import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.demo.api.RemoteServiceConsumer;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceConsumer extends RemoteServiceConsumer {
    public static void main(String[] args) {
        invoke2("kinrpc-demo-grpc", ProtocolType.GRPC.getName());
    }
}
