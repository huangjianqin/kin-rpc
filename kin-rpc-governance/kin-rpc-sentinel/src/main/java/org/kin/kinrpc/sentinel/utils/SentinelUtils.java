package org.kin.kinrpc.sentinel.utils;

import com.alibaba.csp.sentinel.config.SentinelConfig;
import com.alibaba.csp.sentinel.util.StringUtil;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.StringUtils;
import org.kin.kinrpc.Invocation;
import org.kin.kinrpc.sentinel.origin.DefaultOriginParser;
import org.kin.kinrpc.sentinel.origin.OriginParser;

import java.util.List;

/**
 * @author huangjianqin
 * @date 2023/8/5
 */
public final class SentinelUtils {
    /** sentinel配置名-资源名是否使用前缀 */
    private static final String RES_NAME_WITH_PREFIX_KEY = "csp.sentinel.kinrpc.resource.use.prefix";
    /** sentinel配置名-provider资源名前缀 */
    private static final String PROVIDER_RES_NAME_PREFIX_KEY = "csp.sentinel.kinrpc.resource.provider.prefix";
    /** sentinel配置名-consumer资源名前缀 */
    private static final String CONSUMER_RES_NAME_PREFIX_KEY = "csp.sentinel.kinrpc.resource.consumer.prefix";
    /** sentinel配置名-服务唯一标识是否带上group和version */
    private static final String GSV_ENABLED = "csp.sentinel.kinrpc.gsv.enabled";


    /** provider origin (caller) parser instance */
    private static final OriginParser ORIGIN_PARSER;
    /** 资源名是否使用前缀 */
    private static final boolean USE_PREFIX;
    /** 服务唯一标识是否带上group和version */
    private static final boolean ENABLE_INTERFACE_GROUP_AND_VERSION;
    /** provider资源名前缀 */
    private static final String PROVIDER_PREFIX;
    /** consumer资源名前缀 */
    private static final String CONSUMER_PREFIX;

    static {
        List<OriginParser> originParsers = ExtensionLoader.getExtensions(OriginParser.class);
        if (CollectionUtils.isNonEmpty(originParsers)) {
            ORIGIN_PARSER = originParsers.get(0);
        } else {
            ORIGIN_PARSER = new DefaultOriginParser();
        }

        USE_PREFIX = Boolean.TRUE.toString().equalsIgnoreCase(SentinelConfig.getConfig(RES_NAME_WITH_PREFIX_KEY));
        ENABLE_INTERFACE_GROUP_AND_VERSION = Boolean.TRUE.toString().equalsIgnoreCase(SentinelConfig.getConfig(GSV_ENABLED));

        String providerPrefixConfig = SentinelConfig.getConfig(PROVIDER_RES_NAME_PREFIX_KEY);
        PROVIDER_PREFIX = StringUtil.isNotBlank(providerPrefixConfig) ? providerPrefixConfig : "kinrpc:provider:";

        String consumerPrefixConfig = SentinelConfig.getConfig(CONSUMER_RES_NAME_PREFIX_KEY);
        CONSUMER_PREFIX = StringUtil.isNotBlank(consumerPrefixConfig) ? consumerPrefixConfig : "kinrpc:consumer:";
    }

    private SentinelUtils() {
    }

    /**
     * 返回provider资源名前缀
     *
     * @return provider资源名前缀
     */
    public static String getProviderResNamePrefix() {
        if (isUsePrefix()) {
            return PROVIDER_PREFIX;
        }
        return "";
    }

    /**
     * 返回consumer资源名前缀
     *
     * @return consumer资源名前缀
     */
    public static String getConsumerResNamePrefix() {
        if (isUsePrefix()) {
            return CONSUMER_PREFIX;
        }
        return "";
    }

    /**
     * 返回服务方法资源名
     *
     * @param invocation         kinrpc invocation
     * @param useGroupAndVersion 服务唯一标识是否带上group和version
     * @return 服务方法资源名
     */
    private static String getMethodResName(Invocation invocation, boolean useGroupAndVersion) {
        StringBuilder buf = new StringBuilder(64);
        String interfaceResource = useGroupAndVersion ? invocation.service() : invocation.serviceName();
        buf.append(interfaceResource)
                .append(":")
                .append(invocation.handlerName())
                .append("(");
        boolean isFirst = true;
        for (Class<?> clazz : invocation.method().getParameterTypes()) {
            if (!isFirst) {
                buf.append(",");
            }
            buf.append(clazz.getName());
            isFirst = false;
        }
        buf.append(")");
        return buf.toString();
    }

    /**
     * 返回服务方法资源名
     *
     * @param invocation kinrpc invocation
     * @param prefix     资源名前缀
     * @return 服务方法资源名
     */
    public static String getMethodResName(Invocation invocation, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }

        return prefix + getMethodResName(invocation, isEnableInterfaceGroupAndVersion());
    }

    /**
     * 返回服务接口资源名
     *
     * @param invocation kinrpc invocation
     * @param prefix     资源名前缀
     * @return 服务接口资源名
     */
    public static String getInterfaceResName(Invocation invocation, String prefix) {
        if (StringUtils.isBlank(prefix)) {
            prefix = "";
        }
        return prefix + (isEnableInterfaceGroupAndVersion() ? invocation.service() : invocation.serviceName());
    }

    //getter
    public static boolean isUsePrefix() {
        return USE_PREFIX;
    }

    public static Boolean isEnableInterfaceGroupAndVersion() {
        return ENABLE_INTERFACE_GROUP_AND_VERSION;
    }

    public static OriginParser getOriginParser() {
        return ORIGIN_PARSER;
    }
}
