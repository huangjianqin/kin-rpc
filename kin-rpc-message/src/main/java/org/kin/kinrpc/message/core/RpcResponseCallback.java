package org.kin.kinrpc.message.core;

import java.io.Serializable;

/**
 * 消息request response后, 响应返回回调接口
 *
 * @author huangjianqin
 * @date 2020-06-14
 */
public interface RpcResponseCallback<R extends Serializable> {
    RpcResponseCallback<Serializable> EMPTY = new RpcResponseCallback<Serializable>() {
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
