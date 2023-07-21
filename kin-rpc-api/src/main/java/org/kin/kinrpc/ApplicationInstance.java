package org.kin.kinrpc;

import org.kin.framework.utils.NetUtils;

import java.util.Map;

/**
 * @author huangjianqin
 * @date 2023/7/18
 */
public interface ApplicationInstance {
    /**
     * 返回ip:port地址
     *
     * @return ip:port地址
     */
    default String address() {
        return NetUtils.getIpPort(host(), port());
    }

    /**
     * 返回application instance host
     *
     * @return application instance host
     */
    String host();

    /**
     * 返回application instance port
     *
     * @return application instance port
     */
    int port();

    /**
     * 返回application instance metadata
     *
     * @return application instance metadata
     */
    Map<String, String> metadata();

    /**
     * 返回application instance schema
     *
     * @return application instance schema
     */
    String scheme();

    /**
     * 返回应用服务元数据版本
     *
     * @return 应用服务元数据版本
     */
    String revision();

    /**
     * 返回应用实例元数据
     *
     * @param key 元数据key
     * @return
     */
    default String metadata(String key) {
        return metadata(key, "");
    }

    /**
     * 返回应用实例元数据
     *
     * @param key          元数据key
     * @param defaultValue 缺省默认值
     * @return
     */
    default String metadata(String key, String defaultValue) {
        return metadata().getOrDefault(key, defaultValue);
    }
}
