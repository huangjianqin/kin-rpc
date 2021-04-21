package org.kin.kinrpc.rpc.invoker;

import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Url;

/**
 * Created by 健勤 on 2017/2/11.
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {
    protected final Url url;

    protected AbstractInvoker(Url url) {
        this.url = url;
    }
}
