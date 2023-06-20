package org.kin.kinrpc.cluster;

/**
 * @author huangjianqin
 * @date 2019/7/2
 */
public class InvokerNotFoundException extends RuntimeException {
    public InvokerNotFoundException() {
        this("", "");
    }

    public InvokerNotFoundException(String serviceName, String methedName) {
        super(String.format("cannot find valid invoker(serviceName='%s', methedName='%s')", serviceName, methedName));
    }
}
