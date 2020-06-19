package org.kin.kinrpc.message.core;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-14
 *
 * 消息处理返回回调接口
 */
public interface RpcResponseCallback<R extends Serializable> {
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

    /**
     * 消息处理完并返回
     */
    void onSuccess(R message);

    /**
     * 消息处理完并返回, 但遇到错误
     */
    void onFail(Throwable e);
}
