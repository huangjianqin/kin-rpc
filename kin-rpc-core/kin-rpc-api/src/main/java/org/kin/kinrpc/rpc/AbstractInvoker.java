package org.kin.kinrpc.rpc;

import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.common.Url;

/**
 * Created by 健勤 on 2017/2/11.
 */
public abstract class AbstractInvoker<T> implements Invoker<T> {
    /** 构造invoker实例的url */
    protected final Url url;

    protected AbstractInvoker(Url url) {
        this.url = url;
    }

    @Override
    public final Url url() {
        return url;
    }
}
