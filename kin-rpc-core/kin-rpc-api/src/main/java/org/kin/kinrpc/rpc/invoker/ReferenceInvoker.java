package org.kin.kinrpc.rpc.invoker;


import org.kin.kinrpc.rpc.AsyncInvoker;
import org.kin.kinrpc.rpc.RpcResponse;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RpcCallErrorException;
import org.kin.kinrpc.rpc.exception.RpcRetryException;
import org.kin.kinrpc.rpc.exception.UnknownRpcResponseStateCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

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

    @Override
    public final Object invoke(String methodName, Object... params) throws Exception {
        try {
            Future<RpcResponse> future = invoke0(methodName, params);
            RpcResponse rpcResponse = future.get();
            if (rpcResponse != null) {
                switch (rpcResponse.getState()) {
                    case SUCCESS:
                        return rpcResponse.getResult();
                    case RETRY:
                        throw new RpcRetryException(rpcResponse.getInfo(), getServiceName(), methodName, params);
                    case ERROR:
                        throw new RpcCallErrorException(rpcResponse.getInfo());
                    default:
                        throw new UnknownRpcResponseStateCodeException(rpcResponse.getState().getCode());
                }
            } else {
                throw new RpcCallErrorException("no rpc response");
            }
        } catch (InterruptedException e) {
            log.error("pending result interrupted >>> {}", e.getMessage());
            throw e;
        } catch (ExecutionException e) {
            log.error("pending result execute error >>> {}", e.getMessage());
            throw e;
        }
    }

    @Override
    public final Class<T> getInterface() {
        try {
            return (Class<T>) Class.forName(url.getInterfaceN());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public final Future<RpcResponse> invokeAsync(String methodName, Object... params) {
        return invoke0(methodName, params);
    }

    /**
     * @param methodName 方法名
     * @param params     参数
     * @return 调用结果
     */
    protected abstract Future<RpcResponse> invoke0(String methodName, Object... params);
}