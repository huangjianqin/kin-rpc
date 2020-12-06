package org.kin.kinrpc.cluster;

/**
 * Router冲突异常
 *
 * @author huangjianqin
 * @date 2020/9/27
 */
public class RouterConflictException extends RuntimeException {
    public RouterConflictException(Class<? extends Router> class1,
                                   Class<? extends Router> class2) {
        super(String.format("router name conflict! %s, %s", class1.getName(), class2.getName()));
    }
}
