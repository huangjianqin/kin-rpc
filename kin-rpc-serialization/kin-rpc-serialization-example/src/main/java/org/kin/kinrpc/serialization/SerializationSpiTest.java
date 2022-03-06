package org.kin.kinrpc.serialization;

import org.kin.framework.utils.ExtensionLoader;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializationSpiTest {
    public static void main(String[] args) {
        System.out.println(ExtensionLoader.getExtension(Serialization.class, "my"));
    }
}
