package org.kin.kinrpc.bootstrap;

import org.kin.framework.utils.SPI;

/**
 * @author huangjianqin
 * @date 2023/7/12
 */
@SPI
public interface KinRpcBootstrapListener {
    /**
     * 定义{@link KinRpcBootstrap#start()}后的额外处理逻辑
     *
     * @param bootstrap {@link KinRpcBootstrap}实例
     */
    default void onStarted(KinRpcBootstrap bootstrap) {
        //default do nothing
    }

    /**
     * 定义{@link KinRpcBootstrap#destroy()}后的额外处理逻辑
     *
     * @param bootstrap {@link KinRpcBootstrap}实例
     */
    default void onDestroyed(KinRpcBootstrap bootstrap) {
        //default do nothing
    }
}
