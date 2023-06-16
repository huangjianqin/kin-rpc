package org.kin.kinrpc.demo.rpc;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.serialization.SerializationType;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class AddableProvider {
    public static ServiceConfig<Addable> config() {
        return Services.service(new Adder(), Addable.class)
                .serialization(SerializationType.JSON)
                .version("001")
                .actorLike();
    }
}
