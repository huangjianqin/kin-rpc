package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/6/25
 */
public class Services {
    private Services() {

    }

    public static ServiceConfig service(Object ref, Class<?> interfaceClass) {
        return new ServiceConfig(ref, interfaceClass);
    }
}
