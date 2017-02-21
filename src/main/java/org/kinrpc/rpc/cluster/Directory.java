package org.kinrpc.rpc.cluster;

import org.kinrpc.rpc.invoker.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Directory {
    List<ReferenceInvoker> list();
    void destroy();
}
