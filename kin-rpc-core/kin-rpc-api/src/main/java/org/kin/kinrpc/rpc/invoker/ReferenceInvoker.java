package org.kin.kinrpc.rpc.invoker;


import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by 健勤 on 2017/2/15.
 */
public abstract class ReferenceInvoker<T> extends AbstractInvoker<T> implements AsyncInvoker<T> {
    protected static final Logger log = LoggerFactory.getLogger(ReferenceInvoker.class);

    public ReferenceInvoker(Url url) {
        super(url);
    }

    @Override
    public final Url url() {
        return url;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final Class<T> getInterface() {
        try {
            return (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw new RpcCallErrorException(e);
        }
    }
}