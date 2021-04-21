package org.kin.kinrpc.transport.kinrpc;

import org.kin.kinrpc.rpc.common.Url;

/**
 * @author huangjianqin
 * @date 2021/4/19
 */
public abstract class AbstractExecutorFactory implements ExecutorFactory {
    protected AbstractExecutorFactory(Url url, int port) {
        //do nothing
    }
}
