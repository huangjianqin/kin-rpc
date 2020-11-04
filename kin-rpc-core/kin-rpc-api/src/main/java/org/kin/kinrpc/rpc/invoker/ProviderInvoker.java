package org.kin.kinrpc.rpc.invoker;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.rpc.KinRpcUtils;
import org.kin.kinrpc.rpc.common.Constants;
import org.kin.kinrpc.rpc.common.Url;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author huangjianqin
 * @date 2019-08-22
 */
public abstract class ProviderInvoker<T> extends AbstractInvoker<T> {
    protected static final Logger log = LoggerFactory.getLogger(ProviderInvoker.class);
    private final Class<T> interfaceC;
    /** 流控 */
    private RateLimiter rateLimiter;

    protected ProviderInvoker(Url url, Class<T> interfaceC) {
        super(url);
        this.interfaceC = interfaceC;
        int rate = Integer.parseInt(url.getParam(Constants.RATE_KEY));
        rateLimiter = RateLimiter.create(rate);
    }

    @Override
    public final Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        log.debug("service '{}' method '{}' invoking...", getServiceName(), methodName);
        //流控
        if (!rateLimiter.tryAcquire()) {
            throw new RateLimitException(KinRpcUtils.generateInvokeMsg(getServiceName(), methodName, params));
        }
        return doInvoke(methodName, isVoid, params);
    }

    /**
     * rpc调用方法真正实现
     *
     * @param methodName 方法名
     * @param isVoid     对于返回值为void的方法, 直接返回, 不阻塞, service端不用管这个参数
     * @param params     方法参数
     * @return 返回方法结果(rpc调用)
     * @throws Throwable 异常
     */
    protected abstract Object doInvoke(String methodName, boolean isVoid, Object... params) throws Throwable;

    /**
     * 设置流控
     */
    public final void setRate(double rate) {
        if (rate > 0) {
            rateLimiter.setRate(rate);
        }
    }

    /**
     * @return 流控量
     */
    public final double getRate() {
        return rateLimiter.getRate();
    }

    @Override
    public final Class<T> getInterface() {
        return interfaceC;
    }

    @Override
    public final Url url() {
        return null;
    }

    @Override
    public final boolean isAvailable() {
        return true;
    }

    @Override
    public final void destroy() {

    }
}
