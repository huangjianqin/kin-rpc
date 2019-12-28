package org.kin.kinrpc.rpc;

import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2019-12-28
 */
public class RPC {
    public static volatile int parallelism = SysUtils.getSuitableThreadNum() * 2 - 1;
    private RPC() {
    }
}
