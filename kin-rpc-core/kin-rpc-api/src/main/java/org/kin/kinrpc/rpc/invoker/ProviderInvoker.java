package org.kin.kinrpc.rpc.invoker;

import com.google.common.util.concurrent.RateLimiter;
import org.kin.kinrpc.rpc.exception.RateLimitException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.StringJoiner;

/**
 * @author huangjianqin
 * @date 2019-08-22
 */
public abstract class ProviderInvoker<T> extends AbstractInvoker<T> {
    protected static final Logger log = LoggerFactory.getLogger(ProviderInvoker.class);
    private final Class<T> interfaceC;
    /** 流控 */
    private RateLimiter rateLimiter;

    protected ProviderInvoker(String serviceName, Class<T> interfaceC, int rate) {
        super(serviceName);
        this.interfaceC = interfaceC;
        rateLimiter = RateLimiter.create(rate);
    }

    @Override
    public Object invoke(String methodName, boolean isVoid, Object... params) throws Throwable {
        log.debug("service '{}' method '{}' invoking...", getServiceName(), methodName);
        //流控
        if (!rateLimiter.tryAcquire()) {
            //抛异常, 外部捕获异常并立即返回给reference, 让其重试
            StringBuffer sb = new StringBuffer();
            StringJoiner sj = new StringJoiner(", ");
            sb.append("(");
            for (Object param : params) {
                sj.add(param.getClass().getName());
            }
            sb.append(sj.toString());
            sb.append(")");
            throw new RateLimitException(getServiceName().concat("$").concat(methodName).concat(sb.toString()));
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
    public abstract Object doInvoke(String methodName, boolean isVoid, Object... params) throws Throwable;

    /**
     * 设置流控
     */
    public void setRate(double rate) {
        if (rate > 0) {
            rateLimiter.setRate(rate);
        }
    }

    /**
     * 获取流控
     */
    public double getRate() {
        return rateLimiter.getRate();
    }

    @Override
    public Class<T> getInterface() {
        return interfaceC;
    }
}
