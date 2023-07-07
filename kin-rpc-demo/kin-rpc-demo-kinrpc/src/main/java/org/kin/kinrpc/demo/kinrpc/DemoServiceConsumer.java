package org.kin.kinrpc.demo.kinrpc;

import org.kin.kinrpc.config.ProtocolType;
import org.kin.kinrpc.demo.api.RemoteServiceConsumer;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceConsumer extends RemoteServiceConsumer {
    public static void main(String[] args) {
        invoke2("kinrpc-demo-kinrpc", ProtocolType.KINRPC.getName());
    }
}
