package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.constants.ReferenceConstants;

/**
 * 服务方法配置, 如果不配置, 则直接取reference config
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class MethodConfig extends AbstractConfig {
    /**
     * 方法名称
     * 不支持方法重载
     */
    private String name;
    /**
     * rpc call timeout(ms)
     */
    private int timeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
    /** 是否服务调用粘黏 */
    private boolean sticky;

    public static MethodConfig create(String name) {
        return new MethodConfig().name(name);
    }

    private MethodConfig() {
    }

    @Override
    protected void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(name), "method name must be not blank");
        check(timeout > 0, "method rpc call timeout must be greater than 0");
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public MethodConfig name(String name) {
        this.name = name;
        return this;
    }

    public int getTimeout() {
        return timeout;
    }

    public MethodConfig timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public int getRetries() {
        return retries;
    }

    public MethodConfig retries(int retries) {
        this.retries = retries;
        return this;
    }

    public MethodConfig unableRetry() {
        this.retries = -1;
        return this;
    }

    public boolean isAsync() {
        return async;
    }

    public MethodConfig async() {
        this.async = true;
        return this;
    }

    public MethodConfig async(boolean async) {
        this.async = async;
        return this;
    }

    public boolean isSticky() {
        return sticky;
    }

    public MethodConfig sticky() {
        this.sticky = true;
        return this;
    }

    public MethodConfig sticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    @Override
    public String toString() {
        return "MethodConfig{" +
                "name='" + name + '\'' +
                ", timeout=" + timeout +
                ", retries=" + retries +
                ", async=" + async +
                ", sticky=" + sticky +
                '}';
    }
}
