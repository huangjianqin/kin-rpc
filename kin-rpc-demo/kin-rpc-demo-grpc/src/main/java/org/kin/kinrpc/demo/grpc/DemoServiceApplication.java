package org.kin.kinrpc.demo.grpc;

import org.kin.kinrpc.config.ServerConfig;
import org.kin.kinrpc.demo.api.Constants;
import org.kin.kinrpc.demo.api.RemoteServiceApplication;

/**
 * @author huangjianqin
 * @date 2023/7/4
 */
public class DemoServiceApplication extends RemoteServiceApplication {
    public static void main(String[] args) {
        export2(ServerConfig.grpc(Constants.SERVER_PORT1));
    }
}
