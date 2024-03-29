package org.kin.kinrpc.transport;

/**
 * transport操作监听器
 * @author huangjianqin
 * @date 2023/5/30
 */
public interface TransportOperationListener {
    /** 默认do nothing实现 */
    TransportOperationListener DEFAULT = new TransportOperationListener() {
    };
    /** transport操作成功触发 */
    default void onComplete() {

    }

    /**
     * transport操作失败触发
     *
     * @param cause 抛出的异常
     */
    default void onFailure(Throwable cause) {

    }
}
