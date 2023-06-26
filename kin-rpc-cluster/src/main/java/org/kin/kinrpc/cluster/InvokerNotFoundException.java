package org.kin.kinrpc.cluster;

import org.kin.kinrpc.RpcException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class InvokerNotFoundException extends RpcException {

    public InvokerNotFoundException(String handler) {
        super(String.format("can not find valid invoker(handler='%s')", handler));
    }
}
