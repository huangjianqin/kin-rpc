package org.kin.kinrpc.demo.rpc.reference;

import org.kin.kinrpc.demo.rpc.service.Return2;
import org.kin.kinrpc.rpc.Notifier;

/**
 * @author huangjianqin
 * @date 2020/11/19
 */
public class Return2Notifier implements Notifier<Return2> {
    public static final Return2Notifier N = new Return2Notifier();

    @Override
    public void onRpcCallSuc(Return2 obj) {
        System.out.println(obj.toString());
    }

    @Override
    public void handlerException(Throwable throwable) {
        throwable.printStackTrace();
    }
}
