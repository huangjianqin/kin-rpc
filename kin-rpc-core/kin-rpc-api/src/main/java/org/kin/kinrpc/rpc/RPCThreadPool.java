package org.kin.kinrpc.rpc;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;
import org.kin.framework.utils.SysUtils;

import java.util.concurrent.*;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RPCThreadPool {
    public static final ThreadManager THREADS = new ThreadManager(
            new ForkJoinPool(SysUtils.getSuitableThreadNum() * 2 -1,
                    ForkJoinPool.defaultForkJoinWorkerThreadFactory,
                    null,
                    true
                    ), 3, new SimpleThreadFactory("rpc-schedule"));

}
