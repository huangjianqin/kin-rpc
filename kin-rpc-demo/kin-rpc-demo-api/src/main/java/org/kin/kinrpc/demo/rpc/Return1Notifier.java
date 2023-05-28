package org.kin.kinrpc.demo.rpc;

import org.kin.kinrpc.rpc.Notifier;

/**
 * @author huangjianqin
 * @date 2020/11/16
 */
public class Return1Notifier implements Notifier<Return1> {
    public static final Return1Notifier N = new Return1Notifier();

    @Override
    public void onRpcCallSuc(Return1 obj) {
        System.out.println(obj.toString());
    }

    @Override
    public void handleException(Throwable throwable) {
        throwable.printStackTrace();
    }
}