package org.kin.kinrpc.message.core;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-14
 */
public interface RpcResponseCallback {
    RpcResponseCallback EMPTY = new RpcResponseCallback() {
        @Override
        public void onSuccess(Serializable message) {
            //do nothing
        }

        @Override
        public void onFail(Throwable e) {
            //do nothing
        }
    };

    void onSuccess(Serializable message);

    void onFail(Throwable e);
}
