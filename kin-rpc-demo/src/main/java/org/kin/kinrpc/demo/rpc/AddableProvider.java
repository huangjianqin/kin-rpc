package org.kin.kinrpc.demo.rpc;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.serializer.SerializerType;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class AddableProvider {
    public static ServiceConfig<Addable> config() {
        return Services.service(new Adder(), Addable.class)
                .serializer(SerializerType.JSON)
                .version("001")
                .actorLike();
    }
}