package org.kin.kinrpc.cluster.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class CannotFindInvokerException extends RuntimeException {
    public CannotFindInvokerException() {
        this("");
    }

    public CannotFindInvokerException(String methedName) {
        super("cannot find valid invoker '".concat(methedName).concat("'"));
    }
}
