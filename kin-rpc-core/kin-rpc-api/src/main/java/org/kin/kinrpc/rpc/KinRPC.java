package org.kin.kinrpc.rpc;

import org.kin.framework.utils.SysUtils;

/**
 * @author huangjianqin
 * @date 2019-12-28
 */
public class KinRPC {
    public static volatile int PARALLELISM = SysUtils.getSuitableThreadNum() * 2 - 1;

    private KinRPC() {
    }
}
