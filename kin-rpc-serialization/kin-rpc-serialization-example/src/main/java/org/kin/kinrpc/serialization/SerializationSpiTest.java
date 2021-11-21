package org.kin.kinrpc.serialization;

import org.custom.serialization.MySerialization;

/**
 * @author huangjianqin
 * @date 2020/9/27
 */
public class SerializationSpiTest {
    public static void main(String[] args) {
        System.out.println(Serializations.INSTANCE.getSerializationType(MySerialization.class));
    }
}
