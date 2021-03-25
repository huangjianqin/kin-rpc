package org.kin.kinrpc.serialization;

/**
 * Created by 健勤 on 2017/2/9.
 */
public class Hessian2SerializeTest {
    public static void main(String[] args) {
        SerializeTestBase
                .builder(SerializationType.HESSIAN2)
                .run();
    }
}


