package org.kin.kinrpc.registry;


import org.kin.kinrpc.rpc.invoker.AbstractReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Directory {
    List<AbstractReferenceInvoker> list();

    String getServiceName();

    void destroy();
}
