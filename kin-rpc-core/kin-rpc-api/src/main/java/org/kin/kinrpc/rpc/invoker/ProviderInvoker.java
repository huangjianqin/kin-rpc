package org.kin.kinrpc.rpc.invoker;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.common.Constants;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2019-08-22
 */
public abstract class ProviderInvoker extends AbstractInvoker {
    protected static final Logger log = LoggerFactory.getLogger(ProviderInvoker.class);
    //限流
    private RateLimiter rateLimiter;

    protected ProviderInvoker(String serviceName, int rate) {
        super(serviceName);
        rateLimiter = RateLimiter.create(rate);
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        log.debug("service '{}' method '{}' invoking...", getServiceName(), methodName);
        //限流
        if (!rateLimiter.tryAcquire()) {
            //抛异常, 外部捕获异常并立即返回给reference, 让其重试
            throw new RateLimitException();
        }
        return doInvoke(methodName, isVoid, params);
    }

    public abstract Object doInvoke(String methodName, boolean isVoid, Object... params) throws Throwable;

    /**
     * 设置限流
     */
    public void setRate(double rate) {
        if (rate > 0) {
            rateLimiter.setRate(rate);
        }
    }

    /**
     * 获取限流
     */
    public double getRate() {
        return rateLimiter.getRate();
    }
}
