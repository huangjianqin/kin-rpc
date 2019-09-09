package org.kin.kinrpc.rpc.invoker;

/**
 * @author huangjianqin
 * @date 2019-09-09
 */
public interface JavassistMethodInvoker {
    Object invoke(Object... params) throws Exception;
}
