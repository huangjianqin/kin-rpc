package org.kin.kinrpc.registry;


import org.kin.kinrpc.rpc.invoker.impl.ReferenceInvoker;

import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Directory {
    List<ReferenceInvoker> list();

    void discover(List<String> addresses);

    String getServiceName();

    void destroy();
}
