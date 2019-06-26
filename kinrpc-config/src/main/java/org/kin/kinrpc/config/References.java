package org.kin.kinrpc.config;

/**
 * @author huangjianqin
 * @date 2019/6/25
 */
public class References {
    private References(){

    }

    public static <T> ReferenceConfig<T> reference(Class<T> interfaceClass) {
        return new ReferenceConfig(interfaceClass);
    }
}
