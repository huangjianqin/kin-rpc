package org.kin.kinrpc.cluster;

import org.kin.kinrpc.RpcException;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class InvokerNotFoundException extends RpcException {

    private static final long serialVersionUID = 2315174445588284519L;

    public InvokerNotFoundException(String handler) {
        super(String.format("can not find any available invoker for handler '%s'", handler));
    }
}
