package org.kin.kinrpc.rpc;

/**
 * async rpc call 事件通知
 *
 * @param <T> rpc接口返回结果 todo 目前假设rpc回调结果对应的类不会重复
 * @author huangjianqin
 * @date 2020/11/13
 */
public interface Notifier<T> {
    /**
     * rpc call返回调用结果, 逻辑处理
     *
     * @param obj rpc接口返回结果
     */
    void onRpcCallSuc(T obj);

    /**
     * 处理rpc call期间的异常
     */
    void handlerException(Throwable throwable);
}
