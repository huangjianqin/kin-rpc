package org.kin.kinrpc.rpc.invoker;

/**
 * Created by 健勤 on 2017/2/11.
 */
public interface Invoker {
    /**
     * @param isVoid 对于返回值为void的方法, 直接返回, 不阻塞, service端不用管这个参数
     */
    Object invoke(String methodName, boolean isVoid, Object... params) throws Exception;
}
