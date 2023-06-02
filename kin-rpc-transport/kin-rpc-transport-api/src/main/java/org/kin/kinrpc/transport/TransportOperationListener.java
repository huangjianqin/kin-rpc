package org.kin.kinrpc.transport;

/**
 * transport操作监听器
 * @author huangjianqin
 * @date 2023/5/30
 */
public interface TransportOperationListener {
    /** transport操作成功触发 */
    default void onComplete() {

    }

    /** transport操作失败触发 */
    default void onFailure(Throwable t){

    }
}
