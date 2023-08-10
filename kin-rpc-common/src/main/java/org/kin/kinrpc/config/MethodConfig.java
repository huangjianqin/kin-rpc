package org.kin.kinrpc.config;

import org.kin.framework.utils.StringUtils;

import java.util.Objects;

/**
 * 服务方法配置, 如果不配置, 则直接取reference config
 *
 * @author huangjianqin
 * @date 2023/6/16
 */
public class MethodConfig extends AttachableConfig {
    /**
     * 方法名称
     * 不支持方法重载
     */
    private String name;
    /** rpc call timeout(ms) */
    private Integer timeout;
    /** 失败后重试次数 */
    private Integer retries;
    /** 是否异步调用 */
    private Boolean async;
    /** 是否服务调用粘黏 */
    private Boolean sticky;
    /** 服务调用缓存类型 */
    private String cache;
    /** 是否开启参数调用 */
    private Boolean validation;

    public static MethodConfig create(String name) {
        return new MethodConfig().name(name);
    }

    private MethodConfig() {
    }

    @Override
    public void checkValid() {
        super.checkValid();
        check(StringUtils.isNotBlank(name), "method name must be not blank");
        check(timeout > 0, "method rpc call timeout must be greater than 0");
    }

    @Override
    public void initDefaultConfig() {
        super.initDefaultConfig();
        if (Objects.isNull(timeout)) {
            timeout = DefaultConfig.DEFAULT_METHOD_TIMEOUT;
        }

        if (Objects.isNull(retries)) {
            retries = DefaultConfig.DEFAULT_METHOD_RETRIES;
        }

        if (Objects.isNull(async)) {
            async = DefaultConfig.DEFAULT_METHOD_ASYNC;
        }

        if (Objects.isNull(sticky)) {
            sticky = DefaultConfig.DEFAULT_METHOD_STICKY;
        }

        if (Objects.isNull(validation)) {
            validation = DefaultConfig.DEFAULT_METHOD_VALIDATION;
        }
    }

    //setter && getter
    public String getName() {
        return name;
    }

    public MethodConfig name(String name) {
        this.name = name;
        return this;
    }

    public Integer getTimeout() {
        return timeout;
    }

    public MethodConfig timeout(int timeout) {
        this.timeout = timeout;
        return this;
    }

    public Integer getRetries() {
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

    public Boolean isAsync() {
        return Objects.nonNull(async) ? async : false;
    }

    public MethodConfig async() {
        this.async = true;
        return this;
    }

    public MethodConfig async(boolean async) {
        this.async = async;
        return this;
    }

    public Boolean isSticky() {
        return Objects.nonNull(sticky) ? sticky : false;
    }

    public MethodConfig sticky() {
        this.sticky = true;
        return this;
    }

    public MethodConfig sticky(boolean sticky) {
        this.sticky = sticky;
        return this;
    }

    public String getCache() {
        return cache;
    }

    public MethodConfig cache(String cache) {
        this.cache = cache;
        return this;
    }

    public MethodConfig cache(CacheType cacheType) {
        return cache(cacheType.getName());
    }

    public Boolean isValidation() {
        return validation;
    }

    public MethodConfig validation(Boolean validation) {
        this.validation = validation;
        return this;
    }

    public MethodConfig validation() {
        return validation(true);
    }

    @Override
    public String toString() {
        return "MethodConfig{" +
                "name='" + name + '\'' +
                ", timeout=" + timeout +
                ", retries=" + retries +
                ", async=" + async +
                ", sticky=" + sticky +
                "cache='" + cache + '\'' +
                ", validation=" + validation +
                ", " + super.toString() +
                '}';
    }
}
