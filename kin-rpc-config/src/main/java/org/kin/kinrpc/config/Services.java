package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/6/25
 */
public class Services {
    private Services() {

    }

    public static <T> ServiceConfig<T> service(T ref, Class<T> interfaceClass) {
        return new ServiceConfig<>(ref, interfaceClass);
    }
}
