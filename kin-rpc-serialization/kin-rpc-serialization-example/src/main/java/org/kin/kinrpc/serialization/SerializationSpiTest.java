package org.kin.kinrpc.serialization;

import org.kin.kinrpc.rpc.common.RpcExtensionLoader;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializationSpiTest {
    public static void main(String[] args) {
        System.out.println(RpcExtensionLoader.LOADER.getExtension(Serialization.class, "my"));
    }
}
