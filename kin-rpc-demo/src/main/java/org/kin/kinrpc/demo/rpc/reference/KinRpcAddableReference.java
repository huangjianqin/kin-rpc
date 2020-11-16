package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.config.ReferenceConfig;
import org.kin.kinrpc.demo.rpc.service.Addable;
import org.kin.kinrpc.rpc.common.Constants;

/**
 * @author huangjianqin
 * @date 2019/7/1
 */
public class KinRpcAddableReference {
    public static void main(String[] args) throws Exception {
        ReferenceConfig<Addable> config = AddableReference.config();
        config.urls("kinrpc://0.0.0.0:16888?"
                .concat(Constants.SERVICE_NAME_KEY).concat("=").concat(Addable.class.getName())
                .concat("&")
                .concat(Constants.VERSION_KEY).concat("=").concat("001"));
        AddableReference.roundTest(config);
    }
}
