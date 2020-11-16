package org.kin.kinrpc.demo.rpc.provider;

import org.kin.kinrpc.config.ServiceConfig;
import org.kin.kinrpc.config.Services;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.demo.rpc.service.Adder;
import org.kin.kinrpc.serializer.SerializerType;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class AddableProvider {
    protected static ServiceConfig<Addable> config() {
        return Services.service(new Adder(), Addable.class)
                .serialize(SerializerType.JSON)
                .version("001")
                .actorLike();
    }
}
