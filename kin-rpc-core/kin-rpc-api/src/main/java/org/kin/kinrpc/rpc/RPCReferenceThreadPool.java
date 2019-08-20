package org.kin.kinrpc.rpc;

import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.ThreadManager;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2019-08-20
 */
public class RPCReferenceThreadPool {
    public static final ThreadManager THREADS = new ThreadManager(
            new ThreadPoolExecutor(0, 10, 60L, TimeUnit.SECONDS,
                    new LinkedBlockingQueue<>(), new SimpleThreadFactory("rpcreference-common")),
            3, new SimpleThreadFactory("rpcreference-common-schedule"));
}
