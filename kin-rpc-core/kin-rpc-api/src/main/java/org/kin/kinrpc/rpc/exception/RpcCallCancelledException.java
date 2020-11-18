package org.kin.kinrpc.rpc.exception;

/**
 * @author huangjianqin
 * @date 2020/11/12
 */
public class RpcCallCancelledException extends RuntimeException {
    private static final long serialVersionUID = -5315515498897951360L;

    public RpcCallCancelledException(String errorMsg) {
        super(errorMsg);
    }
}
