package org.kin.kinrpc.rpc.common;

import org.kin.framework.utils.KinServiceLoader;

/**
 * @author huangjianqin
 * @date 2020/9/28
 */
public class RpcServiceLoader {
    public static final KinServiceLoader LOADER = KinServiceLoader.load();

    private RpcServiceLoader() {
    }
}
