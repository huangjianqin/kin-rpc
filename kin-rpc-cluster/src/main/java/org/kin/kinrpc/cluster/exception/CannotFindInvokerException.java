package org.kin.kinrpc.cluster.exception;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class CannotFindInvokerException extends RuntimeException {
    public CannotFindInvokerException() {
        this("", "");
    }

    public CannotFindInvokerException(String serviceName, String methedName) {
        super(String.format("cannot find valid invoker(serviceName='%s', methedName='%s')", serviceName, methedName));
    }
}
