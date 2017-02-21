package org.kinrpc.rpc.cluster.router;

import org.kinrpc.rpc.invoker.ReferenceInvoker;
import java.util.List;

/**
 * Created by 健勤 on 2017/2/15.
 */
public interface Router {
    List<ReferenceInvoker> router(List<ReferenceInvoker> invokers);
}
