package org.kin.kinrpc.rpc.invoker;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.rpc.Invoker;
import org.kin.kinrpc.rpc.RpcUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;

/**
 * 流控 invoker
 *
 * @author huangjianqin
 * @date 2020/11/4
 */
public final class RateLimitInvoker<T> extends WrapInvoker<T> {
    /** 流控 */
    private final RateLimiter rateLimiter;

    public RateLimitInvoker(Invoker<T> wrapper) {
        this(wrapper, Constants.SERVER_REQUEST_THRESHOLD);
    }

    public RateLimitInvoker(Invoker<T> wrapper, double permitsPerSecond) {
        super(wrapper);
        this.rateLimiter = RateLimiter.create(permitsPerSecond);
    }

    @Override
    public Object invoke(String methodName, Object[] params) throws Throwable {
        //简单地添加到任务队列交由上层的线程池去完成服务调用
        //流控
        if (!rateLimiter.tryAcquire()) {
            Url url = wrapper.url();
            throw new RateLimitException(RpcUtils.generateInvokeMsg(url.getServiceName(), methodName, params));
        }
        return super.invoke(methodName, params);
    }
}
