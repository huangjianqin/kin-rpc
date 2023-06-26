package org.kin.kinrpc.config;

import org.kin.kinrpc.constants.ReferenceConstants;

/**
 * 服务方法配置, 如果不配置, 则直接取reference config
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class MethodConfig implements Config{
    /**
     * 方法名称
     * todo 暂时无法做到重载方法的配置
     */
    private String name;
    /**
     * rpc call timeout
     * todo 对于service端, 如果调用超时, 那么仅仅会打印log
     */
    private int timeout = ReferenceConstants.DEFAULT_RPC_CALL_TIMEOUT;
    /** 失败后重试次数 */
    private int retries = ReferenceConstants.DEFAULT_RETRY_TIMES;
    /** 是否异步调用 */
    private boolean async;
    /** 是否服务调用粘黏 */
    private boolean sticky;

    public static MethodConfig create(String name){
        return new MethodConfig().name(name);
    }

    private MethodConfig() {
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
}
