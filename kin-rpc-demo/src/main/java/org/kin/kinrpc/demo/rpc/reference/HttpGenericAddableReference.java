package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.demo.rpc.service.Addable;

/**
 * @author huangjianqin
 * @date 2020/11/18
 */
public class HttpGenericAddableReference {
    public static void main(String[] args) throws Exception {
        ReferenceConfig<Addable> config = AddableReference.config();
        config.urls("http://0.0.0.0:16888").useGeneric();
        AddableReference.roundTest(config);
    }
}
